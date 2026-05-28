package com.shan.mq.amps.connectors.amps.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class OrderMessage {

    private String messageId;

    private String orderId;

    private String customerId;

    private Instant createdTime;

    private String payload;
}