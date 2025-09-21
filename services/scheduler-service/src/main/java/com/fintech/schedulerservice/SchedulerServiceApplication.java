package com.fintech.schedulerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class SchedulerServiceApplication {

    public static void main(String[] args) {
        // Set default timezone to avoid PostgreSQL timezone issues
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        System.setProperty("user.timezone", "America/New_York");
        
        SpringApplication.run(SchedulerServiceApplication.class, args);
    }

}
