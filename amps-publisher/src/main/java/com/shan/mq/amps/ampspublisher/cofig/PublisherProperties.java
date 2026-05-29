package com.shan.mq.amps.ampspublisher.cofig;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "publisher")
public class PublisherProperties {
    private int queueCapacity = 10000;
    private int batchSize = 100;
}