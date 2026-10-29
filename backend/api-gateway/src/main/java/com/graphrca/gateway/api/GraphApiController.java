package com.graphrca.gateway.api;

import com.graphrca.gateway.engine.EngineClient;
import com.graphrca.gateway.model.EdgeMetrics;
import com.graphrca.gateway.model.GraphSnapshot;
import com.graphrca.gateway.model.ServiceMetrics;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api")
public class GraphApiController {
    private final EngineClient engineClient;

    public GraphApiController(EngineClient engineClient) {
        this.engineClient = engineClient;
    }

    @GetMapping("/graph/current")
    public GraphSnapshot currentGraph() {
        return engineClient.currentGraph();
    }

    @GetMapping("/graph/history")
    public List<GraphSnapshot> graphHistory(
            @RequestParam(name = "limit", defaultValue = "50") @Min(1) @Max(500) int limit) {
        return engineClient.graphHistory(limit);
    }

    @GetMapping("/metrics/edges")
    public List<EdgeMetrics> edgeMetrics() {
        return engineClient.edgeMetrics();
    }

    @GetMapping("/services")
    public List<ServiceMetrics> services() {
        return engineClient.services();
    }
}
