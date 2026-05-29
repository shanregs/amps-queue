package com.shan.mq.amps.ampspublisher.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shan.mq.amps.ampspublisher.service.PublishService;
import com.shan.mq.amps.ampspublisher.util.OrderMessageFakerUtil;
import com.shan.mq.amps.connectors.amps.model.OrderMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;


@Slf4j
@Component
public class OrderPublishLoadRunner {

    private final PublishService publishService;
    private final ObjectMapper objectMapper;

    public OrderPublishLoadRunner(PublishService publishService, ObjectMapper objectMapper) {
        this.publishService = publishService;
        this.objectMapper = objectMapper;
    }

    //    @PostConstruct
    public void publish() {
        log.info("Publishing Order Messages");
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 10; i++) {
                executor.submit(() -> {
                    publishMessages();
                });
            }
        }
    }

    private void publishMessages() {
        try {
            OrderMessage orderMessage = OrderMessageFakerUtil.createOrderMessage();
            publishService.submit(objectMapper.writeValueAsString(orderMessage));
        } catch (Exception ex) {
            log.error("Failed to publish message", ex);
        }
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}