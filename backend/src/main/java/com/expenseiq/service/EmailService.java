package com.expenseiq.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public boolean sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(toEmail);
            helper.setSubject("Your ExpenseIQ verification code");
            
            String content = "<div style='font-family: sans-serif; padding: 20px; border: 1px solid #eee; border-radius: 10px; max-width: 500px;'>" +
                    "<h2 style='color: #7c3aed;'>ExpenseIQ Verification</h2>" +
                    "<p>Hello,</p>" +
                    "<p>Your verification code is:</p>" +
                    "<h1 style='background: #f3f4f6; padding: 10px; text-align: center; letter-spacing: 5px; color: #1f2937; border-radius: 8px;'>" + otp + "</h1>" +
                    "<p>This code will expire in 5 minutes.</p>" +
                    "<p>If you didn't request this, please ignore this email.</p>" +
                    "<hr style='border: none; border-top: 1px solid #eee; margin: 20px 0;'>" +
                    "<p style='color: #888; font-size: 12px;'>Powered by ExpenseIQ — Your Smart Finance Manager</p>" +
                    "</div>";

            helper.setText(content, true);
            mailSender.send(message);
            
            log.info("OTP Email sent successfully to {}", toEmail);
            return true;
        } catch (MessagingException e) {
            log.error("Failed to send OTP Email to {}. Error: {}", toEmail, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("SMTP Configuration issue. Please check application.properties. Error: {}", e.getMessage());
            return false;
        }
    }
}
