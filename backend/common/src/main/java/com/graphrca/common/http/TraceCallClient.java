package com.graphrca.common.http;

import com.graphrca.common.dto.ServiceResponse;
import com.graphrca.common.dto.TraceEvent;
import com.graphrca.common.kafka.TraceEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Component
public class TraceCallClient {
    private final RestClient restClient;
    private final TraceEventPublisher eventPublisher;

    public TraceCallClient(RestClient.Builder restClientBuilder, TraceEventPublisher eventPublisher) {
        this.restClient = restClientBuilder.build();
        this.eventPublisher = eventPublisher;
    }

    public ServiceResponse call(String caller, String callee, String url, String traceId) {
        Instant timestamp = Instant.now();
        long startNanos = System.nanoTime();
        String status = "SUCCESS";
        try {
            ServiceResponse response = restClient.get()
                    .uri(url + "/process")
                    .header("X-Trace-Id", traceId)
                    .retrieve()
                    .body(ServiceResponse.class);
            if (response == null) {
                status = "ERROR";
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Downstream service returned an empty response");
            }
            return response;
        } catch (RestClientException exception) {
            status = "ERROR";
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Downstream call to " + callee + " failed", exception);
        } catch (ResponseStatusException exception) {
            status = "ERROR";
            throw exception;
        } finally {
            double latencyMs = (System.nanoTime() - startNanos) / 1_000_000.0;
            eventPublisher.publish(new TraceEvent(traceId, caller, callee, timestamp, latencyMs, status));
        }
    }
}
