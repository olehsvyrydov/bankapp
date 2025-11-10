package com.bank.accounts;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "spring.config.import=optional:configserver:",
    "spring.cloud.config.enabled=false"
})
@ActiveProfiles("test")
class AccountsApplicationTests {

    @Test
    void contextLoads() {
    }
}
