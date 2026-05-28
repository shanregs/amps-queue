package com.shan.mq.amps.connectors.amps.config;

import lombok.Data;

@Data
public class AmpsProperties {

    private String host;

    private int port;

    private String topic;

    private String queue;
}
