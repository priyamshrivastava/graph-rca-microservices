package com.graphrca.engine.model;

import java.time.Instant;

public record EdgeMetrics(
        String id,
        String caller,
        String callee,
        long callCount,
        double callsPerSecond,
        double meanLatencyMs,
        double standardDeviationMs,
        double ewmaLatencyMs,
        double p95LatencyMs,
        double errorRate,
        double ewmaErrorRate,
        double zScore,
        Instant lastSeen,
        String health
) {
}
