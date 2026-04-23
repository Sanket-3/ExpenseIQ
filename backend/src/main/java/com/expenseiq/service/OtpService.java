package com.expenseiq.service;

import com.expenseiq.model.OtpToken;
import com.expenseiq.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;

    @Value("${app.otp.expiration:300000}")
    private long otpExpiration;

    @Value("${app.otp.length:6}")
    private int otpLength;

    public String generateOTP(String identifier) {
        String otp = String.format("%0" + otpLength + "d", new Random().nextInt((int) Math.pow(10, otpLength)));

        OtpToken token = OtpToken.builder()
                .identifier(identifier)
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusSeconds(otpExpiration / 1000))
                .used(false)
                .build();

        otpTokenRepository.save(token);
        return otp;
    }

    public boolean verifyOTP(String identifier, String otp) {
        return otpTokenRepository.findTopByIdentifierAndUsedFalseOrderByExpiresAtDesc(identifier)
                .map(token -> {
                    if (token.getOtp().equals(otp) && token.getExpiresAt().isAfter(LocalDateTime.now())) {
                        token.setUsed(true);
                        otpTokenRepository.save(token);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }
}