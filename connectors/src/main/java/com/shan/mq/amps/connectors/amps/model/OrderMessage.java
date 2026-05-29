package com.shan.mq.amps.connectors.amps.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderMessage {
    private String messageId;
    private String orderId;
    private String customerId;
    private Instant createdTime;
    private String payload;
}