package com.graphrca.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfiguration {
    @Bean
    NewTopic traceEventsTopic(@Value("${trace.topic}") String topicName,
                              @Value("${trace.topic-partitions}") int partitions) {
        return TopicBuilder.name(topicName).partitions(partitions).replicas(1).build();
    }
}
