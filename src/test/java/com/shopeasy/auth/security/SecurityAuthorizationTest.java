package com.shopeasy.auth.security;

import com.shopeasy.auth.entity.AccountStatus;
import com.shopeasy.auth.entity.Session;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.entity.UserRole;
import com.shopeasy.auth.repository.SessionRepository;
import com.shopeasy.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        JwtAuthenticationFilter.clearCache();
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String createActiveSessionUser(String email, String mobile, UserRole role) {
        User user = userRepository.save(User.builder()
                .fullName("Test " + role.name())
                .email(email)
                .mobileNumber(mobile)
                .password("$2a$10$dummyHashedPasswordStringForTests")
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .build());

        String token = jwtUtils.generateToken(user.getEmail(), user.getId());

        sessionRepository.save(Session.builder()
                .user(user)
                .jwtToken(token)
                .isActive(true)
                .loginTime(LocalDateTime.now())
                .expiryTime(LocalDateTime.now().plusDays(1))
                .build());

        return token;
    }

    @Test
    @DisplayName("Public product browsing endpoint should be accessible without any token")
    void testPublicEndpointAccessibleAnonymously() throws Exception {
        mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Protected user profile should reject unauthenticated requests with 401 Unauthorized")
    void testProtectedProfileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin endpoint should reject unauthenticated requests with 401 Unauthorized")
    void testAdminEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin endpoint should reject standard USER role with 403 Forbidden")
    void testAdminEndpointForbiddenForNormalUser() throws Exception {
        String userToken = createActiveSessionUser("normal.user@agrimitra.com", "9811111111", UserRole.USER);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin endpoint should allow ADMIN role with 200 OK")
    void testAdminEndpointAllowedForAdminUser() throws Exception {
        String adminToken = createActiveSessionUser("admin.boss@agrimitra.com", "9822222222", UserRole.ADMIN);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
