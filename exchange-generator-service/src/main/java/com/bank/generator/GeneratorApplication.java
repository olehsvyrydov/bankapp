package com.bank.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.bank.generator", "com.bank.common"})
@EnableScheduling
public class GeneratorApplication {

  public static void main(String[] args) {
    SpringApplication.run(GeneratorApplication.class, args);
  }
}
