package com.shan.mq.amps.ampssubscriber;

import com.shan.mq.amps.ampssubscriber.config.AmpsConnectionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

//@EnableConfigurationProperties({AmpsConnectionProperties.class})
@SpringBootApplication
@ConfigurationPropertiesScan
public class AmpsSubscriberApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmpsSubscriberApplication.class, args);
    }

}
