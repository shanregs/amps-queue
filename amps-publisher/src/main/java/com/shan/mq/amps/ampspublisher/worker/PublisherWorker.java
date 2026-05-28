package com.shan.mq.amps.ampspublisher.worker;

import com.shan.mq.amps.connectors.amps.publisher.AmpsPublisher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublisherWorker {
    private final BlockingQueue<String> publisherQueue;

    private final AmpsPublisher ampsPublisher;

    @Value("${amps.topic}")
    private String queueName;
    @PostConstruct
    public void start() {
        log.info("Starting Publisher Worker on Queue : {}", queueName);
        Thread.startVirtualThread(() -> {
            while (true) {
                try{
                    String payload = publisherQueue.take();
                    ampsPublisher.publish(queueName,payload);
                }catch (Exception ex){
                    log.error("Publish failed",ex);
                }
            }
        });
    }
}
