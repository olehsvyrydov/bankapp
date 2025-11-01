package com.bank.auth.config;

import com.bank.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DatabaseInitChecker implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitChecker.class);

    private final UserRepository userRepository;

    public DatabaseInitChecker(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        log.info("=== DATABASE INITIALIZATION CHECK ===");
        long userCount = userRepository.count();
        log.info("Total users in database: {}", userCount);

        if (userCount == 0) {
            log.error("WARNING: No users found in database! Database might not be initialized properly.");
        } else {
            userRepository.findAll().forEach(user ->
                log.info("User found: {} (role: {}, enabled: {})",
                    user.getUsername(), user.getRoles(), user.isEnabled())
            );
        }
        log.info("=== END DATABASE CHECK ===");
    }
}

