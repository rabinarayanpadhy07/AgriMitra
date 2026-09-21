package com.shopeasy.auth.service;

import com.shopeasy.auth.dto.*;
import com.shopeasy.auth.entity.OtpVerification;
import com.shopeasy.auth.entity.User;
import com.shopeasy.auth.exception.*;
import com.shopeasy.auth.repository.OtpVerificationRepository;
import com.shopeasy.auth.repository.UserRepository;
import com.shopeasy.auth.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final SessionService sessionService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    @Transactional
    public UserProfileResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String mobile = request.getMobileNumber().trim();

        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("Email address is already registered: " + email);
        }

        if (userRepository.existsByMobileNumber(mobile)) {
            throw new UserAlreadyExistsException("Mobile number is already registered: " + mobile);
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ApiException("Password and confirm password do not match");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .mobileNumber(mobile)
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User savedUser = userRepository.save(user);
        logger.info("New user registered successfully with ID: {}, Email: {}", savedUser.getId(), savedUser.getEmail());

        return userService.mapToProfileResponse(savedUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String identifier = request.getIdentifier().trim();

        // Support login by either Email or Mobile Number
        User user = userRepository.findByEmailOrMobileNumber(identifier)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        // Generate JWT
        String token = jwtUtils.generateToken(user.getEmail(), user.getId());
        Date expiryDate = jwtUtils.getExpirationDateFromToken(token);
        LocalDateTime expiryTime = expiryDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        LocalDateTime loginTime = LocalDateTime.now();

        // Create active user session in database
        sessionService.createSession(user, token, expiryTime, httpRequest);
        logger.info("User logged in successfully: {}, session created", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .user(userService.mapToProfileResponse(user))
                .loginTime(loginTime)
                .expiryTime(expiryTime)
                .build();
    }

    @Transactional
    public void logout(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            sessionService.invalidateSession(token);
            logger.info("User logged out, session invalidated for token");
        }
        SecurityContextHolder.clearContext();
    }

    @Transactional
    public String forgotPassword(ForgotPasswordRequest request) {
        String identifier = request.getIdentifier().trim();
        User user = userRepository.findByEmailOrMobileNumber(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("No registered account found with given email or mobile number"));

        // Generate 6-digit OTP
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(10);

        // Mark any previous unused OTPs as used
        otpVerificationRepository.markAllOtpsAsUsedForUser(user);

        OtpVerification otpVerification = OtpVerification.builder()
                .user(user)
                .otpCode(otp)
                .expiryTime(expiryTime)
                .isUsed(false)
                .build();

        otpVerificationRepository.save(otpVerification);

        // Send OTP email
        boolean emailSent = emailService.sendOtpEmail(user.getEmail(), user.getFullName(), otp);
        return emailSent ? null : otp;
    }

    @Transactional(readOnly = true)
    public void verifyOtp(VerifyOtpRequest request) {
        String identifier = request.getIdentifier().trim();
        User user = userRepository.findByEmailOrMobileNumber(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with given email or mobile number"));

        OtpVerification otpRecord = otpVerificationRepository
                .findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new InvalidOtpException("No active OTP found. Please request a new OTP"));

        if (otpRecord.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new InvalidOtpException("OTP has expired. Please request a new OTP");
        }

        if (!otpRecord.getOtpCode().equals(request.getOtp().trim())) {
            throw new InvalidOtpException("Invalid OTP code. Please check and try again");
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String identifier = request.getIdentifier().trim();
        User user = userRepository.findByEmailOrMobileNumber(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with given email or mobile number"));

        OtpVerification otpRecord = otpVerificationRepository
                .findTopByUserAndIsUsedFalseOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new InvalidOtpException("No active OTP found. Please request a new OTP"));

        if (otpRecord.getExpiryTime().isBefore(LocalDateTime.now())) {
            throw new InvalidOtpException("OTP has expired. Please request a new OTP");
        }

        if (!otpRecord.getOtpCode().equals(request.getOtp().trim())) {
            throw new InvalidOtpException("Invalid OTP code. Please check and try again");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ApiException("New password and confirm password do not match");
        }

        // Update password with BCrypt hash
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Mark OTP as used
        otpRecord.setIsUsed(true);
        otpVerificationRepository.save(otpRecord);

        // Invalidate all active sessions for this user
        sessionService.invalidateAllUserSessions(user);
        logger.info("Password reset successfully for user: {}. All active sessions invalidated.", user.getEmail());
    }

    @Transactional
    public void changePassword(String currentUserEmail, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ApiException("Current password does not match");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ApiException("New password and confirm password do not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ApiException("New password cannot be the same as the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        logger.info("Password changed successfully for user: {}", currentUserEmail);
    }
}
