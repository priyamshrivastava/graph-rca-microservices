package com.graphrca.common.dto;

import java.time.Instant;

public record FaultResponse(FaultType type, int magnitude, Instant expiresAt) {
}
