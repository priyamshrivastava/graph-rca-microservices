package com.graphrca.engine.graph;

import com.graphrca.common.dto.TraceEvent;
import com.graphrca.engine.baseline.RollingBaseline;
import com.graphrca.engine.model.EdgeMetrics;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

final class EdgeState {
    private final String caller;
    private final String callee;
    private final RollingBaseline baseline;
    private final Deque<Instant> recentCalls = new ArrayDeque<>();
    private long callCount;
    private long errorCount;
    private Instant lastSeen;
    private double latestLatency;
    private double latestZScore;

    EdgeState(String caller, String callee, int baselineWindow, double ewmaAlpha) {
        this.caller = caller;
        this.callee = callee;
        this.baseline = new RollingBaseline(baselineWindow, ewmaAlpha);
    }

    synchronized void record(TraceEvent event, int rateWindowSeconds) {
        boolean failed = !"SUCCESS".equalsIgnoreCase(event.status());
        latestZScore = baseline.sampleCount() == 0 ? 0 : baseline.zScore(event.latencyMs());
        callCount++;
        if (failed) {
            errorCount++;
        }
        latestLatency = event.latencyMs();
        lastSeen = event.timestamp();
        baseline.add(event.latencyMs(), failed);
        recentCalls.addLast(event.timestamp());
        discardExpiredCalls(rateWindowSeconds);
    }

    synchronized EdgeMetrics metrics(int rateWindowSeconds, double unhealthyErrorRate, double unhealthyZScore,
                                      int minimumBaselineSamples) {
        discardExpiredCalls(rateWindowSeconds);
        boolean baselineReady = baseline.sampleCount() >= minimumBaselineSamples;
        String health = baseline.errorRate() >= unhealthyErrorRate
                || (baselineReady && Math.abs(latestZScore) >= unhealthyZScore)
                ? "UNHEALTHY"
                : "HEALTHY";
        return new EdgeMetrics(
                caller + "->" + callee, caller, callee, callCount,
                recentCalls.size() / (double) rateWindowSeconds,
                baseline.meanLatencyMs(), baseline.standardDeviationMs(), baseline.ewmaLatencyMs(),
                baseline.p95LatencyMs(), baseline.errorRate(), baseline.ewmaErrorRate(), latestZScore,
                lastSeen, health
        );
    }

    private void discardExpiredCalls(int rateWindowSeconds) {
        Instant cutoff = Instant.now().minusSeconds(rateWindowSeconds);
        recentCalls.removeIf(timestamp -> timestamp.isBefore(cutoff));
    }

    synchronized double currentLatency() {
        return latestLatency;
    }

    synchronized long callCount() {
        return callCount;
    }

    synchronized long errorCount() {
        return errorCount;
    }

    synchronized Instant lastSeen() {
        return lastSeen;
    }

    synchronized double averageLatency() {
        return baseline.meanLatencyMs();
    }

    synchronized double errorRate() {
        return baseline.errorRate();
    }

    synchronized boolean hasRecentActivity(Duration freshness) {
        return lastSeen != null && lastSeen.isAfter(Instant.now().minus(freshness));
    }
}
