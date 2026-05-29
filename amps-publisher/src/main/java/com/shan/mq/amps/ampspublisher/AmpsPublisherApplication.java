package com.shan.mq.amps.ampspublisher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class AmpsPublisherApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmpsPublisherApplication.class, args);
    }

}
