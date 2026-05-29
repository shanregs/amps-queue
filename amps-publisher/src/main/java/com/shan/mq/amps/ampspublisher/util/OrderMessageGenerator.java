package com.shan.mq.amps.ampspublisher.util;

import com.shan.mq.amps.connectors.amps.model.OrderMessage;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class OrderMessageGenerator {

    private static final String[] PRODUCTS = {
            "Laptop",
            "Keyboard",
            "Mouse",
            "Monitor",
            "Mobile",
            "Tablet",
            "Camera"
    };

    private static final String[] STATUS = {
            "CREATED",
            "VALIDATED",
            "PROCESSING",
            "COMPLETED"
    };

    private static final String[] PAYMENT_MODES = {
            "CARD",
            "UPI",
            "NETBANKING",
            "WALLET"
    };

    private OrderMessageGenerator() {
    }

    public static OrderMessage generate() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        OrderMessage message = new OrderMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setOrderId("ORD-" + random.nextLong(100000, 999999));
        message.setCustomerId("CUST-" + random.nextLong(10000, 99999));
        message.setCreatedTime(Instant.now());
        message.setPayload(buildPayload(random));
        return message;
    }

    private static String buildPayload(ThreadLocalRandom random) {
        String product = PRODUCTS[random.nextInt(PRODUCTS.length)];
        String status = STATUS[random.nextInt(STATUS.length)];
        String paymentMode = PAYMENT_MODES[random.nextInt(PAYMENT_MODES.length)];
        int quantity = random.nextInt(1, 5);
        double price = Math.round(random.nextDouble(1000, 50000) * 100.0) / 100.0;

        return """
                {
                  "product":"%s",
                  "quantity":%d,
                  "price":%.2f,
                  "paymentMode":"%s",
                  "status":"%s"
                }
                """.formatted(
                product,
                quantity,
                price,
                paymentMode,
                status
        );
    }
}