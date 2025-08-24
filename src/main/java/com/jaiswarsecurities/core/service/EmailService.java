package com.jaiswarsecurities.core.service;

import com.jaiswarsecurities.core.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Email service for sending various types of emails
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${application.mail.from}")
    private String fromEmail;

    @Value("${application.mail.verification-url}")
    private String verificationUrl;

    @Value("${application.mail.reset-password-url}")
    private String resetPasswordUrl;

    /**
     * Send email verification email
     */
    @Async
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendVerificationEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Verify Your Email - Trading Platform");

            String verificationLink = verificationUrl + "?token=" + user.getEmailVerificationToken();
            String text = String.format(
                "Hello %s,\n\n" +
                "Thank you for registering with our Trading Platform!\n\n" +
                "Please click the link below to verify your email address:\n" +
                "%s\n\n" +
                "This link will expire in 24 hours.\n\n" +
                "If you didn't create this account, please ignore this email.\n\n" +
                "Best regards,\n" +
                "Trading Platform Team",
                user.getFirstName() != null ? user.getFirstName() : user.getUsername(),
                verificationLink
            );

            message.setText(text);
            mailSender.send(message);

            log.info("Verification email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", user.getEmail(), e);
            throw e; // Re-throw the exception to trigger retry
        }
    }

    /**
     * Send password reset email
     */
    @Async
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendPasswordResetEmail(User user, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Password Reset - Trading Platform");

            String resetLink = resetPasswordUrl + "?token=" + resetToken;
            String text = String.format(
                "Hello %s,\n\n" +
                "We received a request to reset your password for your Trading Platform account.\n\n" +
                "Please click the link below to reset your password:\n" +
                "%s\n\n" +
                "This link will expire in 1 hour.\n\n" +
                "If you didn't request this password reset, please ignore this email.\n\n" +
                "Best regards,\n" +
                "Trading Platform Team",
                user.getFirstName() != null ? user.getFirstName() : user.getUsername(),
                resetLink
            );

            message.setText(text);
            mailSender.send(message);

            log.info("Password reset email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
            throw e; // Re-throw the exception to trigger retry
        }
    }

    /**
     * Send welcome email after successful verification
     */
    @Async
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendWelcomeEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Welcome to Trading Platform!");

            String text = String.format(
                "Hello %s,\n\n" +
                "Welcome to our Trading Platform! Your email has been successfully verified.\n\n" +
                "You can now start trading and managing your portfolio.\n\n" +
                "If you have any questions, please don't hesitate to contact our support team.\n\n" +
                "Happy trading!\n\n" +
                "Best regards,\n" +
                "Trading Platform Team",
                user.getFullName()
            );

            message.setText(text);
            mailSender.send(message);

            log.info("Welcome email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", user.getEmail(), e);
            throw e; // Re-throw the exception to trigger retry
        }
    }

    /**
     * Send account locked notification
     */
    @Async
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public void sendAccountLockedEmail(User user) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject("Account Locked - Trading Platform");

            String text = String.format(
                "Hello %s,\n\n" +
                "Your Trading Platform account has been temporarily locked due to multiple failed login attempts.\n\n" +
                "For security reasons, your account will be automatically unlocked in 30 minutes.\n\n" +
                "If this wasn't you, please contact our support team immediately.\n\n" +
                "Best regards,\n" +
                "Trading Platform Security Team",
                user.getFullName()
            );

            message.setText(text);
            mailSender.send(message);

            log.info("Account locked notification sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send account locked email to: {}", user.getEmail(), e);
            throw e; // Re-throw the exception to trigger retry
        }
    }
}