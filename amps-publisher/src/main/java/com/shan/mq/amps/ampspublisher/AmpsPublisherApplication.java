package com.shan.mq.amps.ampspublisher;

import com.shan.mq.amps.ampspublisher.cofig.AmpsConnectionProperties;
import com.shan.mq.amps.ampspublisher.cofig.PublisherProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/*@EnableConfigurationProperties({
        AmpsConnectionProperties.class,
        PublisherProperties.class
})*/
@ConfigurationPropertiesScan
@SpringBootApplication
public class AmpsPublisherApplication {

    public static void main(String[] args) {
        SpringApplication.run(AmpsPublisherApplication.class, args);
    }

}
