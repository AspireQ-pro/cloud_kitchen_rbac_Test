package com.cloudkitchen.rbac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
    org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
public class RbacServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RbacServiceApplication.class, args);
    }
}