package com.mapreduce.manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling

public class ManagerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ManagerServiceApplication.class, args);
        System.out.println("Manager Service is running...");
    }
} 