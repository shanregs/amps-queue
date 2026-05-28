package com.shan.mq.amps.ampspublisher.cofig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

@Configuration
public class QueueConfig {

    @Bean
    public BlockingQueue<String> publisherQueue(PublisherProperties publisherProperties) {

        return new ArrayBlockingQueue<>(publisherProperties.getQueueCapacity());
    }
}
