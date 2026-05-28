package com.shan.mq.amps.ampspublisher.cofig;

import com.crankuptheamps.client.Client;
import com.shan.mq.amps.connectors.amps.factory.AmpsClientFactory;
import com.shan.mq.amps.connectors.amps.publisher.AmpsPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AMPSConfig {


    @Bean
    public Client ampsClient(AmpsConnectionProperties properties) throws Exception {

        return new AmpsClientFactory()
                .createClient(
                        properties.getClientName(),
                        properties.getHost(),
                        properties.getPort());
    }

    @Bean
    public AmpsPublisher ampsPublisher(
            Client client) {

        return new AmpsPublisher(client);
    }
}
