package com.shan.mq.amps.ampssubscriber.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "amps")
public class AmpsConnectionProperties {

    private String clientName;

    private String host;

    private int port;

    private String topic;

    private String queue;
}
