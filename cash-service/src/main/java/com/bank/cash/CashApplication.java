package com.bank.cash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@SpringBootApplication(scanBasePackages = {"com.bank.cash", "com.bank.common"})
@EnableDiscoveryClient
@EnableFeignClients
public class CashApplication {
    public static void main(String[] args) {
        SpringApplication.run(CashApplication.class, args);
    }

    @Bean
    CommandLineRunner logClientRegistrations(ClientRegistrationRepository repository) {
        return args -> {
            ClientRegistration registration = repository.findByRegistrationId("cash-service");
            if (registration == null) {
                System.out.println("WARN: Client registration 'cash-service' not found");
            } else {
                System.out.println("INFO: Loaded client registration 'cash-service' with scopes " + registration.getScopes());
            }
        };
    }
}
