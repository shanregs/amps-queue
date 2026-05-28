package com.shan.mq.amps.ampspublisher.runner;

import com.shan.mq.amps.ampspublisher.service.PublishService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
public class PublishLoadRunner {

    private final PublishService publishService;

    @PostConstruct
    public void run() {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 10000; i++) {
                executor.submit(() -> {
                    try {
                        publishService.submit("""
                                {
                                  "messageId":"%s"
                                }
                                """.formatted(UUID.randomUUID()));
                    }
                    catch (Exception ignored) {
                    }
                });
            }
        }
    }
}