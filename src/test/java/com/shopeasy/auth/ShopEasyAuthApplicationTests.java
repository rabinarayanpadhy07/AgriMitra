package com.shopeasy.auth;

import com.shopeasy.auth.dto.LoginRequest;
import com.shopeasy.auth.dto.RegisterRequest;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.repository.UserRepository;
import com.shopeasy.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ShopEasyAuthApplicationTests {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void contextLoads() {
        assertNotNull(passwordEncoder);
        assertNotNull(authService);
        assertNotNull(userRepository);
    }

    @Test
    void testPasswordBCryptHashing() {
        String rawPassword = "SecurePassword@2026";
        String encoded = passwordEncoder.encode(rawPassword);

        assertNotNull(encoded);
        assertNotEquals(rawPassword, encoded);
        assertTrue(passwordEncoder.matches(rawPassword, encoded));
        assertFalse(passwordEncoder.matches("WrongPassword@123", encoded));
    }
}
