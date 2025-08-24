package com.jaiswarsecurities.core.service;

import com.jaiswarsecurities.core.dto.auth.*;
import com.jaiswarsecurities.core.entity.Role;
import com.jaiswarsecurities.core.entity.User;
import com.jaiswarsecurities.core.exception.BusinessException;
import com.jaiswarsecurities.core.exception.ResourceNotFoundException;
import com.jaiswarsecurities.core.repository.RoleRepository;
import com.jaiswarsecurities.core.repository.UserRepository;
import com.jaiswarsecurities.core.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;

    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        log.info("Authenticating user: {}", request.getUsernameOrEmail());
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsernameOrEmail(),
                    request.getPassword()
                )
            );

            String accessToken = jwtUtils.generateAccessToken(authentication);
            String refreshToken = jwtUtils.generateRefreshToken(authentication.getName());

            // Update last login date
            User user = userRepository.findByUsernameOrEmail(
                request.getUsernameOrEmail(),
                request.getUsernameOrEmail()
            ).orElseThrow(() -> new ResourceNotFoundException("User not found"));

            user.setLastLoginDate(LocalDateTime.now());
            user.setFailedLoginAttempts(0);
            userRepository.save(user);

            return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900000L) // 15 minutes
                .user(mapToUserInfo(user))
                .build();

        } catch (Exception e) {
            // Handle failed login attempt
            handleFailedLoginAttempt(request.getUsernameOrEmail());
            throw new BusinessException("Invalid username or password");
        }
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email is already registered");
        }

        // Create new user
        User user = User.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phoneNumber(request.getPhoneNumber())
            .enabled(true)
            .accountLocked(false)
            .emailVerified(false)
            .tradingAccountNumber(generateTradingAccountNumber())
            .emailVerificationToken(UUID.randomUUID().toString())
            .failedLoginAttempts(0)
            .build();

        // Assign default role
        Role traderRole = roleRepository.findByName(Role.TRADER)
            .orElseThrow(() -> new BusinessException("Default role not found"));
        
        Set<Role> roles = new HashSet<>();
        roles.add(traderRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        
        // Send verification email
        emailService.sendEmailVerification(savedUser.getEmail(), savedUser.getEmailVerificationToken());

        // Generate tokens
        String accessToken = jwtUtils.generateAccessToken(savedUser.getUsername());
        String refreshToken = jwtUtils.generateRefreshToken(savedUser.getUsername());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(900000L)
            .user(mapToUserInfo(savedUser))
            .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        
        if (!jwtUtils.validateToken(refreshToken)) {
            throw new BusinessException("Invalid refresh token");
        }

        String username = jwtUtils.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccessToken = jwtUtils.generateAccessToken(username);
        String newRefreshToken = jwtUtils.generateRefreshToken(username);

        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .tokenType("Bearer")
            .expiresIn(900000L)
            .user(mapToUserInfo(user))
            .build();
    }

    @Transactional
    public void logout(String username) {
        log.info("Logging out user: {}", username);
        // In a production system, you might maintain a blacklist of tokens
        // For now, we just log the logout event
    }

    private void handleFailedLoginAttempt(String usernameOrEmail) {
        userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
            .ifPresent(user -> {
                int attempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(attempts);
                
                if (attempts >= 5) {
                    user.setAccountLocked(true);
                    log.warn("Account locked for user: {} after {} failed attempts", 
                        user.getUsername(), attempts);
                }
                
                userRepository.save(user);
            });
    }

    private String generateTradingAccountNumber() {
        String prefix = "JAI";
        String timestamp = String.valueOf(System.currentTimeMillis());
        return prefix + timestamp.substring(timestamp.length() - 8);
    }

    private AuthResponse.UserInfo mapToUserInfo(User user) {
        return AuthResponse.UserInfo.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .tradingAccountNumber(user.getTradingAccountNumber())
            .roles(user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList()))
            .kycStatus(user.getKycStatus().toString())
            .riskProfile(user.getRiskProfile().toString())
            .build();
    }
}
