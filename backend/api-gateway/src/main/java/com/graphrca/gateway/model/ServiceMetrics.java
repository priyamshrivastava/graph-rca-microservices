package com.graphrca.gateway.model;

import java.time.Instant;

public record ServiceMetrics(String id, String name, String health, long calls, double callsPerSecond,
                             double averageLatencyMs, double errorRate, Instant lastSeen) {
}
