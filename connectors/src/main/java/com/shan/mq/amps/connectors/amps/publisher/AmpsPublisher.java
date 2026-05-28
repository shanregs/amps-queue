package com.shan.mq.amps.connectors.amps.publisher;

import com.crankuptheamps.client.Client;

public class AmpsPublisher implements MessagePublisher{
    private final Client client;

    public AmpsPublisher(Client client) {
        this.client = client;
    }

    @Override
    public void publish(
            String topic,
            String payload) throws Exception {

        client.publish(topic, payload);
    }
}
