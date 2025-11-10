package com.bank.transfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.bank.transfer", "com.bank.common"})
@EnableDiscoveryClient
@EnableFeignClients
public class TransferApplication {

  public static void main(String[] args) {
    SpringApplication.run(TransferApplication.class, args);
  }
}
