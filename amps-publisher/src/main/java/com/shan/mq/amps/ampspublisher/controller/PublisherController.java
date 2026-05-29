package com.shan.mq.amps.ampspublisher.controller;

import com.shan.mq.amps.ampspublisher.runner.OrderPublishLoadRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/orders")
public class PublisherController {

    private final OrderPublishLoadRunner orderPublishLoadRunner;

    public PublisherController(OrderPublishLoadRunner orderPublishLoadRunner) {
        this.orderPublishLoadRunner = orderPublishLoadRunner;
    }

    @PostMapping("/publish")
    public void publish() {
        log.info("Publishing Order Messages");
        orderPublishLoadRunner.publish();
    }

}
