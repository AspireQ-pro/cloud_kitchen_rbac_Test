package com.cloudkitchen.rbac;

import java.util.TimeZone;
import java.io.FileInputStream;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
public class RbacServiceApplication {
    public static void main(String[] args) {
        // Load .env file manually
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(".env"));
            props.forEach((key, value) -> {
                if (System.getenv(key.toString()) == null) {
                    System.setProperty(key.toString(), value.toString());
                }
            });
            System.out.println("Loaded .env file with " + props.size() + " variables");
        } catch (Exception e) {
            System.out.println("Failed to load .env file: " + e.getMessage());
        }
        
        // Set default timezone to India Standard Time
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(RbacServiceApplication.class, args);
    }
}
