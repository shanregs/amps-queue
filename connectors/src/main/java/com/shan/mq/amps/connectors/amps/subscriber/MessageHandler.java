package com.shan.mq.amps.connectors.amps.subscriber;

@FunctionalInterface
public interface MessageHandler {

    void handle(String payload);

}