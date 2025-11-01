package com.bank.transfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@SpringBootApplication(scanBasePackages = {"com.bank.transfer", "com.bank.common"})
@EnableDiscoveryClient
@EnableFeignClients
public class TransferApplication {
    public static void main(String[] args) {
        SpringApplication.run(TransferApplication.class, args);
    }

    @Bean
    CommandLineRunner logTransferClient(ClientRegistrationRepository repository) {
        return args -> {
            ClientRegistration registration = repository.findByRegistrationId("transfer-service");
            if (registration == null) {
                System.out.println("WARN: Client registration 'transfer-service' not found");
            } else {
                System.out.println("INFO: Loaded client registration 'transfer-service' with scopes " + registration.getScopes());
            }
        };
    }
}
