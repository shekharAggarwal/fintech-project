package com.fintech.transactionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class TransactionServiceApplication {

    public static void main(String[] args) {
        // Set default timezone to avoid PostgreSQL timezone issues
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        System.setProperty("user.timezone", "Asia/Kolkata");
        
        SpringApplication.run(TransactionServiceApplication.class, args);
    }

}
