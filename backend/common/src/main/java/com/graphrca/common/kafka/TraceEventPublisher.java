package com.graphrca.common.kafka;

import com.graphrca.common.dto.TraceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionException;

@Component
public class TraceEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(TraceEventPublisher.class);

    private final KafkaTemplate<String, TraceEvent> kafkaTemplate;
    private final String topic;

    public TraceEventPublisher(KafkaTemplate<String, TraceEvent> kafkaTemplate,
                               @Value("${trace.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(TraceEvent event) {
        kafkaTemplate.send(topic, event.traceId(), event)
                .whenComplete((result, failure) -> {
                    if (failure != null) {
                        Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                                ? failure.getCause()
                                : failure;
                        logger.error("Failed to publish trace event for trace {}", event.traceId(), cause);
                    }
                });
    }
}
