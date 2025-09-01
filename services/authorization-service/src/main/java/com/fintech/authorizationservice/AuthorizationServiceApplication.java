package com.fintech.authorizationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;

import java.util.TimeZone;

@SpringBootApplication(exclude = {FlywayAutoConfiguration.class})
public class AuthorizationServiceApplication {

	public static void main(String[] args) {
		// Set default timezone to avoid PostgreSQL timezone issues
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
		System.setProperty("user.timezone", "Asia/Kolkata");

		SpringApplication.run(AuthorizationServiceApplication.class, args);
	}

}
