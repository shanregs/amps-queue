package com.shan.mq.amps.connectors.amps.subscriber;

import com.crankuptheamps.client.Client;
import com.crankuptheamps.client.Message;
import com.crankuptheamps.client.MessageStream;

public class AmpsSubscriber {
    private final Client client;

    public AmpsSubscriber(Client client) {
        this.client = client;
    }
    public void subscribe(String queueName,MessageHandler handler)    throws Exception {
        MessageStream stream = client.subscribe(queueName);
        while (stream.hasNext()) {
            Message message = stream.next();
            handler.handle(message.getData());
        }
    }
}
