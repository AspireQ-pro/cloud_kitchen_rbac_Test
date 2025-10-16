package com.cloudkitchen.rbac.config;

import java.util.TimeZone;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class JacksonDateTimeConfig {
    
    @PostConstruct
    public void init() {
        // Force set timezone for EC2 deployment
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        System.setProperty("user.timezone", "Asia/Kolkata");
    }
    
    @Bean
    public TimeZone timeZone() {
        TimeZone defaultTimeZone = TimeZone.getTimeZone("Asia/Kolkata");
        TimeZone.setDefault(defaultTimeZone);
        return defaultTimeZone;
    }
}