package com.shan.mq.amps.ampspublisher.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;


@Service
@RequiredArgsConstructor
public  class PublishService {
    private final BlockingQueue<String> publisherQueue;

    public void submit(String payload)throws InterruptedException {
        publisherQueue.put(payload);
    }
}
