package com.jaiswarsecurities.core.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.jaiswarsecurities.core.dto.auth.AuthResponse;
import com.jaiswarsecurities.core.dto.auth.LoginRequest;
import com.jaiswarsecurities.core.dto.auth.PasswordResetRequest;
import com.jaiswarsecurities.core.dto.auth.RegisterRequest;
import com.jaiswarsecurities.core.dto.auth.UserProfileDto;
import com.jaiswarsecurities.core.exception.AuthenticationException;
import com.jaiswarsecurities.core.exception.UserAlreadyExistsException;
import com.jaiswarsecurities.core.exception.UserNotFoundException;
import com.jaiswarsecurities.core.model.Role;
import com.jaiswarsecurities.core.model.User;
import com.jaiswarsecurities.core.model.UserStatus;
import com.jaiswarsecurities.core.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Authentication service handling user registration, login, and token management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final ModelMapper modelMapper;
    private final EmailService emailService;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int ACCOUNT_LOCK_DURATION_MINUTES = 30;

    /**
     * Register a new user
     */
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user with username: {}", request.getUsername());

        // Validate password confirmation
        if (!request.isPasswordMatching()) {
            throw new AuthenticationException("Passwords do not match");
        }

        // Check if user already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already exists: " + request.getEmail());
        }

        try {
            // Create new user
            User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phoneNumber(request.getPhoneNumber())
                    .role(Role.TRADER) // Default role
                    .status(UserStatus.PENDING_VERIFICATION)
                    .emailVerified(false)
                    .emailVerificationToken(generateVerificationToken())
                    .build();

            User savedUser = userRepository.save(user);

            // Send verification email
            emailService.sendVerificationEmail(savedUser);

            log.info("User registered successfully: {}", savedUser.getUsername());

            return AuthResponse.builder()
                    .success(true)
                    .message("Registration successful. Please check your email to verify your account.")
                    .userId(savedUser.getId())
                    .username(savedUser.getUsername())
                    .email(savedUser.getEmail())
                    .issuedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error during user registration: {}", e.getMessage(), e);
            throw new AuthenticationException("Registration failed: " + e.getMessage());
        }
    }

    /**
     * Authenticate user and generate tokens
     */
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsernameOrEmail());

        try {
            // Find user
            User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail())
                    .orElseThrow(() -> new UserNotFoundException("User not found: " + request.getUsernameOrEmail()));

            // Check if email is verified
            if (!user.isEmailVerified()) {
                throw new AuthenticationException("Account is not verified. Please check your email to verify your account.");
            }

            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );

            // Check if account is locked after successful authentication
            if (user.isAccountLocked()) {
                throw new LockedException("Account is locked due to multiple failed login attempts. Please try again later.");
            }

            // Reset failed attempts on successful login
            if (user.getFailedLoginAttempts() > 0) {
                user.resetFailedLoginAttempts();
                userRepository.save(user);
            }

            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Generate tokens
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("userId", user.getId().toString());
            extraClaims.put("role", user.getRole().name());
            extraClaims.put("email", user.getEmail());

            String accessToken = jwtService.generateToken(extraClaims, user);
            String refreshToken = jwtService.generateRefreshToken(user);

            log.info("Login successful for user: {}", user.getUsername());

            return AuthResponse.builder()
                    .success(true)
                    .message("Login successful")
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .fullName(user.getFullName())
                    .role(user.getRole())
                    .emailVerified(user.isEmailVerified())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresAt(LocalDateTime.now().plusSeconds(jwtService.getExpirationTime() / 1000))
                    .issuedAt(LocalDateTime.now())
                    .build();

        } catch (BadCredentialsException e) {
            handleFailedLogin(request.getUsernameOrEmail());
            throw new AuthenticationException("Invalid username/email or password");
        } catch (DisabledException e) {
            throw new AuthenticationException("Account is disabled. Please contact support.");
        } catch (LockedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during login: {}", e.getMessage(), e);
            throw new AuthenticationException("Login failed: " + e.getMessage());
        }
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthResponse refreshToken(String refreshToken) {
        log.info("Refreshing access token");

        try {
            // Validate refresh token format
            if (!jwtService.isValidTokenFormat(refreshToken)) {
                throw new AuthenticationException("Invalid refresh token format");
            }

            // Extract username from refresh token
            String username = jwtService.extractUsername(refreshToken);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

            // Validate refresh token
            if (!jwtService.isTokenValid(refreshToken, user)) {
                throw new AuthenticationException("Invalid or expired refresh token");
            }

            // Generate new access token
            Map<String, Object> extraClaims = new HashMap<>();
            extraClaims.put("userId", user.getId().toString());
            extraClaims.put("role", user.getRole().name());
            extraClaims.put("email", user.getEmail());

            String newAccessToken = jwtService.generateToken(extraClaims, user);

            log.info("Access token refreshed for user: {}", user.getUsername());

            return AuthResponse.builder()
                    .success(true)
                    .message("Token refreshed successfully")
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .accessToken(newAccessToken)
                    .refreshToken(refreshToken) // Keep the same refresh token
                    .tokenType("Bearer")
                    .expiresAt(LocalDateTime.now().plusSeconds(jwtService.getExpirationTime() / 1000))
                    .issuedAt(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage(), e);
            throw new AuthenticationException("Token refresh failed: " + e.getMessage());
        }
    }

    /**
     * Verify email address
     */
    public AuthResponse verifyEmail(String token) {
        log.info("Verifying email with token: {}", token);

        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new AuthenticationException("Invalid or expired verification token"));

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        log.info("Email verified successfully for user: {}", user.getUsername());

        return AuthResponse.builder()
          .success(true)
          .message("Email verified successfully. Your account is now active.")
          .build();
    }

    /**
     * Request password reset
     */
    public AuthResponse requestPasswordReset(PasswordResetRequest request) {
        log.info("Password reset requested for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getEmail()));

        // Generate reset token
        String resetToken = generatePasswordResetToken();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetExpires(LocalDateTime.now().plusHours(1)); // 1 hour expiry
        userRepository.save(user);

        // Send reset email
        emailService.sendPasswordResetEmail(user, resetToken);

        log.info("Password reset email sent to: {}", request.getEmail());

        return AuthResponse.builder()
               .success(true)
               .message("Password reset email sent. Please check your email.")
               .build();
    }

    /**
     * Reset password with token
     */
    public AuthResponse resetPassword(String token, String newPassword) {
        log.info("Resetting password with token");

        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new AuthenticationException("Invalid or expired password reset token"));

        // Check if token is expired
        if (user.getPasswordResetExpires() == null
                || user.getPasswordResetExpires().isBefore(LocalDateTime.now())) {
            throw new AuthenticationException("Password reset token has expired");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpires(null);
        userRepository.save(user);

        log.info("Password reset successfully for user: {}", user.getUsername());

        return AuthResponse.builder()
               .success(true)
               .message("Password reset successfully")
               .build();
    }

    /**
     * Get current authenticated user
     */
    public UserProfileDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationException("No authenticated user found");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + username));

        return modelMapper.map(user, UserProfileDto.class);
    }

    /**
     * Handle failed login attempts
     */
    private void handleFailedLogin(String usernameOrEmail) {
        userRepository.findByUsernameOrEmail(usernameOrEmail)
                .ifPresent(user -> {
                    user.incrementFailedLoginAttempts();

                    if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                        user.lockAccount(ACCOUNT_LOCK_DURATION_MINUTES);
                        log.warn("Account locked for user: {} due to {} failed attempts",
                                user.getUsername(), user.getFailedLoginAttempts());
                    }

                    userRepository.save(user);
                });
    }

    /**
     * Generate email verification token
     */
    private String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate password reset token
     */
    private String generatePasswordResetToken() {
        return UUID.randomUUID().toString();
    }
}