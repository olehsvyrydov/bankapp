package com.bank.blocker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.bank.blocker", "com.bank.common"})
@EnableDiscoveryClient
public class BlockerApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlockerApplication.class, args);
    }
}
