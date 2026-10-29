package com.graphrca.common.dto;

public record ServiceResponse(String traceId, String service, String status, String message) {
}
