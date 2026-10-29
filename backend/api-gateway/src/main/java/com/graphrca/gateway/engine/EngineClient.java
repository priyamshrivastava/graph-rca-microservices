package com.graphrca.gateway.engine;

import com.graphrca.gateway.model.EdgeMetrics;
import com.graphrca.gateway.model.GraphSnapshot;
import com.graphrca.gateway.model.ServiceMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@Component
public class EngineClient {
    private static final Logger logger = LoggerFactory.getLogger(EngineClient.class);
    private static final ParameterizedTypeReference<List<GraphSnapshot>> HISTORY_TYPE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<EdgeMetrics>> EDGES_TYPE = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<ServiceMetrics>> SERVICES_TYPE = new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public EngineClient(RestClient.Builder builder, @Value("${engine.base-url}") String engineBaseUrl) {
        this.restClient = builder.baseUrl(engineBaseUrl).build();
    }

    public GraphSnapshot currentGraph() {
        return get("/internal/graph/current", GraphSnapshot.class);
    }

    public List<GraphSnapshot> graphHistory(int limit) {
        return get("/internal/graph/history?limit=" + limit, HISTORY_TYPE);
    }

    public List<EdgeMetrics> edgeMetrics() {
        return get("/internal/metrics/edges", EDGES_TYPE);
    }

    public List<ServiceMetrics> services() {
        return get("/internal/services", SERVICES_TYPE);
    }

    private <T> T get(String path, Class<T> type) {
        try {
            return restClient.get().uri(path).retrieve().body(type);
        } catch (RestClientException exception) {
            logger.error("RCA engine request failed for {}", path, exception);
            throw new ResponseStatusException(BAD_GATEWAY, "RCA engine is unavailable", exception);
        }
    }

    private <T> T get(String path, ParameterizedTypeReference<T> type) {
        try {
            return restClient.get().uri(path).retrieve().body(type);
        } catch (RestClientException exception) {
            logger.error("RCA engine request failed for {}", path, exception);
            throw new ResponseStatusException(BAD_GATEWAY, "RCA engine is unavailable", exception);
        }
    }
}
