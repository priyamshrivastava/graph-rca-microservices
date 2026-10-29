package com.graphrca.gateway.model;

import java.time.Instant;
import java.util.List;

public record GraphSnapshot(Instant capturedAt, List<ServiceMetrics> nodes, List<EdgeMetrics> edges) {
}
