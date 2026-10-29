package com.graphrca.engine.api;

import com.graphrca.engine.graph.ServiceGraph;
import com.graphrca.engine.model.EdgeMetrics;
import com.graphrca.engine.model.GraphSnapshot;
import com.graphrca.engine.model.ServiceMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
public class EngineController {
    private final ServiceGraph serviceGraph;
    private final int defaultHistoryLimit;

    public EngineController(ServiceGraph serviceGraph,
                            @Value("${rca.history.default-limit}") int defaultHistoryLimit) {
        this.serviceGraph = serviceGraph;
        this.defaultHistoryLimit = defaultHistoryLimit;
    }

    @GetMapping("/internal/graph/current")
    public GraphSnapshot currentGraph() {
        return serviceGraph.currentSnapshot();
    }

    @GetMapping("/internal/graph/history")
    public List<GraphSnapshot> graphHistory(@RequestParam(name = "limit", defaultValue = "-1") int limit) {
        return serviceGraph.history(limit < 0 ? defaultHistoryLimit : limit);
    }

    @GetMapping("/internal/metrics/edges")
    public List<EdgeMetrics> edgeMetrics() {
        return serviceGraph.edgeMetrics();
    }

    @GetMapping("/internal/services")
    public List<ServiceMetrics> services() {
        return serviceGraph.serviceMetrics();
    }

    @GetMapping("/internal/status")
    public Map<String, Object> status() {
        Instant lastEvent = serviceGraph.lastEventAt();
        return Map.of("lastEventAt", lastEvent == null ? "" : lastEvent.toString());
    }
}
