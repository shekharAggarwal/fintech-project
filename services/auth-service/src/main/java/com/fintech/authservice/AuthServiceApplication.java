package com.fintech.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class AuthServiceApplication {

    public static void main(String[] args) {
        // Set default timezone to avoid PostgreSQL timezone issues
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        System.setProperty("user.timezone", "Asia/Kolkata");

        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
