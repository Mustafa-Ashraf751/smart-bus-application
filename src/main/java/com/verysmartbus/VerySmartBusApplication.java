package com.verysmartbus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.verysmartbus.email.EmailNotificationProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(EmailNotificationProperties.class)
public class VerySmartBusApplication {

    public static void main(String[] args) {

        SpringApplication.run(VerySmartBusApplication.class, args
        );
    }
}
