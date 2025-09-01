package com.fintech.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.TimeZone;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaRepositories
public class NotificationServiceApplication {

    public static void main(String[] args) {
        // Set default timezone to avoid PostgreSQL timezone issues
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        System.setProperty("user.timezone", "Asia/Kolkata");
        
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

}
