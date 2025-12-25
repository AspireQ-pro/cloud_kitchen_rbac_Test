package com.cloudkitchen.rbac;

import java.util.TimeZone;
import java.io.FileInputStream;
import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication(exclude = {
     org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
 })
@EnableJpaRepositories(basePackages = "com.cloudkitchen.rbac.repository")
@EntityScan(basePackages = "com.cloudkitchen.rbac.domain.entity")
public class RbacServiceApplication {

     private static final Logger logger = LoggerFactory.getLogger(RbacServiceApplication.class);

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
            logger.info("Loaded .env file with {} variables", props.size());
        } catch (Exception e) {
            logger.warn("Failed to load .env file: {}", e.getMessage());
        }
        
        // Set default timezone to India Standard Time
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(RbacServiceApplication.class, args);
    }
}
