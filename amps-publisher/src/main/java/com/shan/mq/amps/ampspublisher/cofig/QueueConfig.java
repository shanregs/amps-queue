package com.shan.mq.amps.ampspublisher.cofig;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Slf4j
@Configuration
public class QueueConfig {

    @Bean
    public BlockingQueue<String> publisherQueue(PublisherProperties publisherProperties) {
        log.info("Creating publisher queue with capacity {}", publisherProperties.getQueueCapacity());
        return new ArrayBlockingQueue<>(publisherProperties.getQueueCapacity());
    }
}
