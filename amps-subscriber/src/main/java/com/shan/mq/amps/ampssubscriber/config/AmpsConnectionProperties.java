package com.shan.mq.amps.connectors.amps.config;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "amps")
public class AmpsConnectionProperties {

    private String clientName;

    private String host;

    private int port;

    private String topic;

    private String queue;
}
