package com.expenseiq.service;

import com.expenseiq.dto.*;
import com.expenseiq.model.User;
import com.expenseiq.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    // ===== REGISTER =====
    public ApiResponse register(RegisterRequest request) {
        String email = request.getEmail();

        // Check if user exists
        if (email != null && userRepository.existsByEmail(email)) {
            return ApiResponse.builder().success(false).message("Email already registered").build();
        }

        // Create user (unverified)
        User user = User.builder()
                .name(request.getName())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .verified(false)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        // Generate and send OTP
        String otp = otpService.generateOTP(email);
        boolean sent = emailService.sendOtpEmail(email, otp);
        
        if (!sent) {
            return ApiResponse.builder()
                    .success(true) // User created, but email failed
                    .message("User created, but verification email could not be sent. Check SMTP settings.")
                    .build();
        }

        return ApiResponse.builder()
                .success(true)
                .message("Registration successful! Please verify with the OTP sent to " + email)
                .build();
    }

    // ===== LOGIN =====
    public ApiResponse login(LoginRequest request) {
        String email = request.getEmail();

        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return ApiResponse.builder().success(false).message("User not found. Please register.").build();
        }

        User user = optionalUser.get();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ApiResponse.builder().success(false).message("Incorrect password").build();
        }

        if (!user.isVerified()) {
            // Resend OTP
            String otp = otpService.generateOTP(email);
            boolean sent = emailService.sendOtpEmail(email, otp);

            if (!sent) {
                return ApiResponse.builder()
                        .success(false)
                        .message("Account needs verification, but email could not be sent. Check SMTP config.")
                        .build();
            }

            return ApiResponse.builder()
                    .success(false)
                    .needsVerification(true)
                    .message("Account not verified. OTP sent to email.")
                    .build();
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT
        String token = generateToken(user);

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", user.getName());
        userData.put("email", user.getEmail());

        return ApiResponse.builder()
                .success(true)
                .message("Login successful!")
                .token(token)
                .user(userData)
                .build();
    }

    // ===== VERIFY OTP =====
    public ApiResponse verifyOtp(OtpVerifyRequest request) {
        String email = request.getEmail();

        if (!otpService.verifyOTP(email, request.getOtp())) {
            return ApiResponse.builder().success(false).message("Invalid or expired OTP").build();
        }

        // Mark user as verified
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (optionalUser.isEmpty()) {
            return ApiResponse.builder().success(false).message("User not found").build();
        }

        User user = optionalUser.get();
        user.setVerified(true);
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String token = generateToken(user);

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", user.getName());
        userData.put("email", user.getEmail());

        return ApiResponse.builder()
                .success(true)
                .message("Account verified!")
                .token(token)
                .user(userData)
                .build();
    }

    // ===== RESEND OTP =====
    public ApiResponse resendOtp(Map<String, String> request) {
        String email = request.get("email");
        String otp = otpService.generateOTP(email);

        boolean sent = emailService.sendOtpEmail(email, otp);

        if (!sent) {
            return ApiResponse.builder()
                    .success(false)
                    .message("Failed to send OTP email. Please check server configuration.")
                    .build();
        }

        return ApiResponse.builder().success(true).message("OTP resent!").build();
    }

    // ===== JWT TOKEN =====
    private String generateToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("name", user.getName())
                .claim("email", user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key)
                .compact();
    }
}
