package com.shopeasy.auth.config;

import com.shopeasy.auth.entity.UserRole;
import com.shopeasy.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;

    @Value("${shopeasy.app.adminBootstrapEmail:}")
    private String adminBootstrapEmail;

    @Override
    public void run(String... args) {
        if (adminBootstrapEmail == null || adminBootstrapEmail.isBlank()) {
            return;
        }

        if (userRepository.countByRole(UserRole.ADMIN) > 0) {
            return;
        }

        userRepository.findByEmail(adminBootstrapEmail.trim().toLowerCase()).ifPresentOrElse(user -> {
            user.setRole(UserRole.ADMIN);
            userRepository.save(user);
            logger.info("Admin bootstrap: promoted user {} to ADMIN", user.getEmail());
        }, () -> logger.warn("Admin bootstrap: no registered user found with email {}", adminBootstrapEmail));
    }
}
