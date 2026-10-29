package com.graphrca.engine.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graphrca.common.dto.TraceEvent;
import com.graphrca.engine.model.EdgeMetrics;
import com.graphrca.engine.model.GraphSnapshot;
import com.graphrca.engine.model.ServiceMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ServiceGraph {
    private final Map<String, EdgeState> edges = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> adjacency = new ConcurrentHashMap<>();
    private final Set<String> configuredServices = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final int baselineWindow;
    private final int rateWindowSeconds;
    private final double ewmaAlpha;
    private final double unhealthyErrorRate;
    private final double unhealthyZScore;
    private final int minimumBaselineSamples;
    private final int historyMaxLimit;
    private final Duration serviceFreshness;
    private volatile Instant lastEventAt;

    public ServiceGraph(ObjectMapper objectMapper, JdbcTemplate jdbcTemplate,
                        @Value("${rca.services}") List<String> services,
                        @Value("${rca.baseline.window-size}") int baselineWindow,
                        @Value("${rca.baseline.ewma-alpha}") double ewmaAlpha,
                        @Value("${rca.metrics.rate-window-seconds}") int rateWindowSeconds,
                        @Value("${rca.thresholds.unhealthy-error-rate}") double unhealthyErrorRate,
                        @Value("${rca.thresholds.unhealthy-z-score}") double unhealthyZScore,
                        @Value("${rca.baseline.minimum-samples}") int minimumBaselineSamples,
                        @Value("${rca.services.freshness-seconds}") long freshnessSeconds,
                        @Value("${rca.history.max-limit}") int historyMaxLimit) {
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.configuredServices.addAll(services);
        this.baselineWindow = baselineWindow;
        this.ewmaAlpha = ewmaAlpha;
        this.rateWindowSeconds = rateWindowSeconds;
        this.unhealthyErrorRate = unhealthyErrorRate;
        this.unhealthyZScore = unhealthyZScore;
        if (minimumBaselineSamples < 1 || minimumBaselineSamples > baselineWindow) {
            throw new IllegalArgumentException("rca.baseline.minimum-samples must be between 1 and the baseline window size");
        }
        this.minimumBaselineSamples = minimumBaselineSamples;
        this.serviceFreshness = Duration.ofSeconds(freshnessSeconds);
        this.historyMaxLimit = historyMaxLimit;
    }

    @KafkaListener(topics = "${trace.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(TraceEvent event) {
        if (event == null || event.caller() == null || event.callee() == null || event.timestamp() == null
                || event.status() == null || !Double.isFinite(event.latencyMs()) || event.latencyMs() < 0
                || !Set.of("SUCCESS", "ERROR").contains(event.status().toUpperCase())) {
            throw new IllegalArgumentException("Trace event has an invalid shape or values");
        }
        EdgeState edge = edges.computeIfAbsent(
                event.caller() + "->" + event.callee(),
                ignored -> new EdgeState(event.caller(), event.callee(), baselineWindow, ewmaAlpha)
        );
        edge.record(event, rateWindowSeconds);
        adjacency.computeIfAbsent(event.caller(), ignored -> ConcurrentHashMap.newKeySet()).add(event.callee());
        adjacency.computeIfAbsent(event.callee(), ignored -> ConcurrentHashMap.newKeySet());
        configuredServices.add(event.caller());
        configuredServices.add(event.callee());
        lastEventAt = event.timestamp();
    }

    public GraphSnapshot currentSnapshot() {
        List<EdgeMetrics> edgeMetrics = edgeMetrics();
        Map<String, List<EdgeMetrics>> adjacent = new LinkedHashMap<>();
        for (String service : configuredServices) {
            adjacent.put(service, new ArrayList<>());
        }
        Map<String, EdgeMetrics> edgesById = new LinkedHashMap<>();
        edgeMetrics.forEach(edge -> edgesById.put(edge.id(), edge));
        adjacency.forEach((caller, callees) -> {
            adjacent.computeIfAbsent(caller, ignored -> new ArrayList<>());
            callees.forEach(callee -> {
                EdgeMetrics metrics = edgesById.get(caller + "->" + callee);
                if (metrics != null) {
                    adjacent.get(caller).add(metrics);
                    adjacent.computeIfAbsent(callee, ignored -> new ArrayList<>()).add(metrics);
                }
            });
        });
        for (EdgeMetrics edge : edgeMetrics) {
            adjacent.computeIfAbsent(edge.caller(), ignored -> new ArrayList<>());
            adjacent.computeIfAbsent(edge.callee(), ignored -> new ArrayList<>());
        }
        List<ServiceMetrics> services = adjacent.entrySet().stream().map(entry -> {
            List<EdgeMetrics> relatedEdges = entry.getValue();
            List<EdgeMetrics> incoming = relatedEdges.stream().filter(edge -> edge.callee().equals(entry.getKey())).toList();
            List<EdgeMetrics> serviceEdges = incoming.isEmpty()
                    ? relatedEdges.stream().filter(edge -> edge.caller().equals(entry.getKey())).toList()
                    : incoming;
            long calls = serviceEdges.stream().mapToLong(EdgeMetrics::callCount).sum();
            double callsPerSecond = serviceEdges.stream().mapToDouble(EdgeMetrics::callsPerSecond).sum();
            double averageLatency = serviceEdges.stream().mapToDouble(EdgeMetrics::meanLatencyMs).average().orElse(0);
            double errorRate = serviceEdges.stream().mapToDouble(EdgeMetrics::errorRate).average().orElse(0);
            Instant seen = relatedEdges.stream().map(EdgeMetrics::lastSeen).filter(value -> value != null)
                    .max(Instant::compareTo).orElse(null);
            String health = seen != null && seen.isAfter(Instant.now().minus(serviceFreshness))
                    && relatedEdges.stream().anyMatch(edge -> edge.health().equals("UNHEALTHY"))
                    ? "UNHEALTHY"
                    : seen != null && seen.isAfter(Instant.now().minus(serviceFreshness)) ? "HEALTHY" : "UNKNOWN";
            return new ServiceMetrics(entry.getKey(), entry.getKey(), health, calls, callsPerSecond,
                    averageLatency, errorRate, seen);
        }).toList();
        return new GraphSnapshot(Instant.now(), services, edgeMetrics);
    }

    public List<EdgeMetrics> edgeMetrics() {
        return edges.values().stream()
                .map(edge -> edge.metrics(rateWindowSeconds, unhealthyErrorRate, unhealthyZScore, minimumBaselineSamples))
                .sorted((left, right) -> left.id().compareTo(right.id()))
                .toList();
    }

    public List<ServiceMetrics> serviceMetrics() {
        return currentSnapshot().nodes();
    }

    public Instant lastEventAt() {
        return lastEventAt;
    }

    public void persistSnapshot() {
        try {
            GraphSnapshot snapshot = currentSnapshot();
            String json = objectMapper.writeValueAsString(snapshot);
            jdbcTemplate.update(
                    "INSERT INTO graph_snapshots (captured_at, graph) VALUES (?, CAST(? AS jsonb))",
                    Timestamp.from(snapshot.capturedAt()), json
            );
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to persist graph snapshot", exception);
        }
    }

    public List<GraphSnapshot> history(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, historyMaxLimit));
        return jdbcTemplate.query(
                "SELECT captured_at, graph::text FROM graph_snapshots ORDER BY captured_at DESC LIMIT ?",
                (resultSet, rowNumber) -> {
                    try {
                        return objectMapper.readValue(resultSet.getString("graph"), GraphSnapshot.class);
                    } catch (Exception exception) {
                        throw new IllegalStateException("Unable to read persisted graph snapshot", exception);
                    }
                },
                safeLimit
        );
    }
}
