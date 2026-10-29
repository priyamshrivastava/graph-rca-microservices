package com.graphrca.engine.persistence;

import com.graphrca.engine.graph.ServiceGraph;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class GraphSnapshotScheduler {
    private final ServiceGraph serviceGraph;

    public GraphSnapshotScheduler(ServiceGraph serviceGraph) {
        this.serviceGraph = serviceGraph;
    }

    @Scheduled(fixedDelayString = "${rca.persistence.snapshot-interval-ms}")
    public void persistSnapshot() {
        serviceGraph.persistSnapshot();
    }
}
