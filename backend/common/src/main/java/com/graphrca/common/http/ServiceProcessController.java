package com.graphrca.common.http;

import com.graphrca.common.dto.ServiceResponse;
import com.graphrca.common.fault.FaultState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ServiceProcessController {
    private final FaultState faultState;
    private final TraceCallClient traceCallClient;

    @Value("${service.name}")
    private String serviceName;

    @Value("${service.next-name:}")
    private String nextServiceName;

    @Value("${service.next-url:}")
    private String nextServiceUrl;

    public ServiceProcessController(FaultState faultState, TraceCallClient traceCallClient) {
        this.faultState = faultState;
        this.traceCallClient = traceCallClient;
    }

    @GetMapping("/process")
    public ServiceResponse process(@RequestHeader(name = "X-Trace-Id", required = false) String requestedTraceId) {
        String traceId = requestedTraceId == null || requestedTraceId.isBlank()
                ? UUID.randomUUID().toString()
                : requestedTraceId;
        faultState.apply();
        if (!nextServiceUrl.isBlank()) {
            traceCallClient.call(serviceName, nextServiceName, nextServiceUrl, traceId);
        }
        return new ServiceResponse(traceId, serviceName, "SUCCESS", serviceName + " completed");
    }
}
