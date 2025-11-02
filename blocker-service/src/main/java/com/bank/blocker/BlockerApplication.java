package com.bank.blocker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.bank.blocker", "com.bank.common"})
public class BlockerApplication {

  public static void main(String[] args) {
    SpringApplication.run(BlockerApplication.class, args);
  }
}
