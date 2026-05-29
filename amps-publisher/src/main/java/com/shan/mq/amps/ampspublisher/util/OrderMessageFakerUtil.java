package com.shan.mq.amps.ampspublisher.util;

import com.github.javafaker.Faker;
import com.shan.mq.amps.connectors.amps.model.OrderMessage;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class OrderMessageFakerUtil {

    private static final Faker FAKER = new Faker();

    private OrderMessageFakerUtil() {
    }

    public static OrderMessage createOrderMessage() {
        OrderMessage orderMessage = new OrderMessage();
        orderMessage.setMessageId(UUID.randomUUID().toString());
        orderMessage.setOrderId(
                "ORD-" + ThreadLocalRandom.current().nextInt(100000, 999999)
        );
        orderMessage.setCustomerId(
                "CUST-" + ThreadLocalRandom.current().nextInt(10000, 99999)
        );
        orderMessage.setCreatedTime(Instant.now());
        orderMessage.setPayload(createPayload());
        return orderMessage;
    }

    private static String createPayload() {
        return """
                {
                  "productName": "%s",
                  "quantity": %d,
                  "price": %.2f,
                  "address": "%s",
                  "paymentMode": "%s",
                  "status": "%s"
                }
                """.formatted(
                FAKER.commerce().productName(),
                ThreadLocalRandom.current().nextInt(1, 10),
                Double.parseDouble(FAKER.commerce().price()),
                FAKER.address().fullAddress(),
                FAKER.business().creditCardType(),
                FAKER.options().option(
                        "CREATED",
                        "PROCESSING",
                        "CONFIRMED"
                )
        );
    }
}