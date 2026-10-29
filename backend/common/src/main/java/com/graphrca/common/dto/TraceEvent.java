package com.graphrca.common.dto;

import java.time.Instant;

public record TraceEvent(
        String traceId,
        String caller,
        String callee,
        Instant timestamp,
        double latencyMs,
        String status
) {
}
