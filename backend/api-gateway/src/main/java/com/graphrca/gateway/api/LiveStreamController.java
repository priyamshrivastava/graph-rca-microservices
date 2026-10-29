package com.graphrca.gateway.api;

import com.graphrca.gateway.engine.EngineClient;
import com.graphrca.gateway.model.GraphSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
public class LiveStreamController {
    private static final Logger logger = LoggerFactory.getLogger(LiveStreamController.class);

    private final EngineClient engineClient;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "graph-rca-sse");
        thread.setDaemon(true);
        return thread;
    });

    public LiveStreamController(EngineClient engineClient) {
        this.engineClient = engineClient;
    }

    @GetMapping(path = "/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        SseEmitter emitter = new SseEmitter(0L);
        AtomicBoolean connected = new AtomicBoolean(true);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            if (!connected.get()) {
                return;
            }
            try {
                GraphSnapshot graph = engineClient.currentGraph();
                emitter.send(SseEmitter.event().name("graph").data(graph));
                emitter.send(SseEmitter.event().name("metrics").data(graph.edges()));
            } catch (IOException exception) {
                connected.set(false);
                logger.debug("SSE client disconnected", exception);
                emitter.complete();
            } catch (RuntimeException exception) {
                logger.error("Unable to publish graph update to SSE client", exception);
                connected.set(false);
                emitter.complete();
            }
        }, 0, 1, TimeUnit.SECONDS);
        emitter.onCompletion(() -> {
            connected.set(false);
            future.cancel(true);
        });
        emitter.onTimeout(() -> {
            connected.set(false);
            future.cancel(true);
            emitter.complete();
        });
        emitter.onError(exception -> {
            connected.set(false);
            future.cancel(true);
        });
        return emitter;
    }
}
