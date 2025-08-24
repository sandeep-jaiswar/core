package com.jaiswarsecurities.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender emailSender;
    
    @Value("${spring.mail.from:noreply@jaiswarsecurities.com}")
    private String fromAddress;
    
    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Async
    public void sendEmailVerification(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Email Verification - Jaiswar Securities");
            message.setText(String.format(
                "Welcome to Jaiswar Securities!\n\n" +
                "Please verify your email address by clicking the link below:\n" +
                "%s/verify-email?token=%s\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "If you didn't create an account with us, please ignore this email.\n\n" +
                "Best regards,\n" +
                "Jaiswar Securities Team",
                frontendUrl, token
            ));
            
            emailSender.send(message);
            log.info("Verification email sent to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Password Reset - Jaiswar Securities");
            message.setText(String.format(
                "Hello,\n\n" +
                "We received a request to reset your password for your Jaiswar Securities account.\n\n" +
                "Click the link below to reset your password:\n" +
                "%s/reset-password?token=%s\n\n" +
                "This link will expire in 1 hour.\n\n" +
                "If you didn't request a password reset, please ignore this email.\n\n" +
                "Best regards,\n" +
                "Jaiswar Securities Team",
                frontendUrl, token
            ));
            
            emailSender.send(message);
            log.info("Password reset email sent to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendTradeConfirmationEmail(String toEmail, String tradingAccountNumber, 
                                         String symbol, String tradeType, int quantity, 
                                         String price, String totalAmount) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Trade Confirmation - Jaiswar Securities");
            message.setText(String.format(
                "Trade Confirmation\n\n" +
                "Account: %s\n" +
                "Symbol: %s\n" +
                "Action: %s\n" +
                "Quantity: %d\n" +
                "Price: ₹%s\n" +
                "Total Amount: ₹%s\n\n" +
                "Your trade has been successfully executed.\n\n" +
                "You can view your portfolio and trading history by logging into your account.\n\n" +
                "Best regards,\n" +
                "Jaiswar Securities Team",
                tradingAccountNumber, symbol, tradeType, quantity, price, totalAmount
            ));
            
            emailSender.send(message);
            log.info("Trade confirmation email sent to: {}", toEmail);
            
        } catch (Exception e) {
            log.error("Failed to send trade confirmation email to {}: {}", toEmail, e.getMessage());
        }
    }
}
