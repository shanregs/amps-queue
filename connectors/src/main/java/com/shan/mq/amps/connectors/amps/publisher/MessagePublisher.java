package com.shan.mq.amps.connectors.amps.publisher;

public interface MessagePublisher {

    void publish(
            String topic,
            String payload) throws Exception;
}