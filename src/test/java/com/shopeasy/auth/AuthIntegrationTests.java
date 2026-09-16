package com.shopeasy.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopeasy.auth.dto.*;
import com.shopeasy.auth.entity.OtpVerification;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.repository.OtpVerificationRepository;
import com.shopeasy.auth.repository.SessionRepository;
import com.shopeasy.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private OtpVerificationRepository otpVerificationRepository;

    @BeforeEach
    void setUp() {
        otpVerificationRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testCompleteAuthLifecycle() throws Exception {
        // 1. Register a new user
        RegisterRequest registerReq = RegisterRequest.builder()
                .fullName("Jane Doe")
                .email("jane.doe@shopeasy.com")
                .mobileNumber("9876543210")
                .password("Password@2026")
                .confirmPassword("Password@2026")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Jane Doe"))
                .andExpect(jsonPath("$.data.email").value("jane.doe@shopeasy.com"))
                .andExpect(jsonPath("$.data.mobileNumber").value("9876543210"));

        // 2. Reject duplicate email registration
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        // 3. Login with Email
        LoginRequest loginEmailReq = LoginRequest.builder()
                .identifier("jane.doe@shopeasy.com")
                .password("Password@2026")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginEmailReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseJson).get("data").get("token").asText();

        // 4. Access protected profile with token
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("jane.doe@shopeasy.com"));

        // 5. Login with Mobile Number
        LoginRequest loginMobileReq = LoginRequest.builder()
                .identifier("9876543210")
                .password("Password@2026")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginMobileReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty());

        // 6. Login with Bad Credentials returns generic message
        LoginRequest badLogin = LoginRequest.builder()
                .identifier("jane.doe@shopeasy.com")
                .password("WrongPassword@123")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        // 7. Logout and verify session invalidation
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 8. Attempt accessing protected resource with logged-out token -> must be rejected!
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        // 9. Forgot password flow
        ForgotPasswordRequest forgotReq = ForgotPasswordRequest.builder()
                .identifier("jane.doe@shopeasy.com")
                .build();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        User user = userRepository.findByEmail("jane.doe@shopeasy.com").orElseThrow();
        OtpVerification otpRecord = otpVerificationRepository
                .findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(user)
                .orElseThrow();
        String generatedOtp = otpRecord.getOtpCode();

        // 10. Verify OTP
        VerifyOtpRequest verifyOtpReq = VerifyOtpRequest.builder()
                .identifier("jane.doe@shopeasy.com")
                .otp(generatedOtp)
                .build();

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyOtpReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 11. Reset Password
        ResetPasswordRequest resetReq = ResetPasswordRequest.builder()
                .identifier("jane.doe@shopeasy.com")
                .otp(generatedOtp)
                .newPassword("NewPassword@2026#")
                .confirmPassword("NewPassword@2026#")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 12. Login with New Password
        LoginRequest newLogin = LoginRequest.builder()
                .identifier("jane.doe@shopeasy.com")
                .password("NewPassword@2026#")
                .build();

        MvcResult newLoginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String newToken = objectMapper.readTree(newLoginResult.getResponse().getContentAsString())
                .get("data").get("token").asText();

        // 13. Change Password while logged in
        ChangePasswordRequest changeReq = ChangePasswordRequest.builder()
                .currentPassword("NewPassword@2026#")
                .newPassword("ChangedPassword@2026!")
                .confirmPassword("ChangedPassword@2026!")
                .build();

        mockMvc.perform(put("/api/auth/change-password")
                        .header("Authorization", "Bearer " + newToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
