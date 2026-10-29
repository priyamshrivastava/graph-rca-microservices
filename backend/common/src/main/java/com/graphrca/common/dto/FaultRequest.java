package com.graphrca.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record FaultRequest(
        @NotNull FaultType type,
        @Min(1) int magnitude,
        @Min(1) int durationSec
) {
}
