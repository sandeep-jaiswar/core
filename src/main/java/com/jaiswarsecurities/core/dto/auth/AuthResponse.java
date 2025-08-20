package com.jaiswarsecurities.core.dto.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jaiswarsecurities.core.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Authentication response DTO containing user info and tokens
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private UUID userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private Role role;
    private boolean emailVerified;

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime issuedAt;

    private String message;
    private boolean success;

    // Convenience constructors
    public static AuthResponse success(String message) {
        return AuthResponse.builder()
                .success(true)
                .message(message)
                .issuedAt(LocalDateTime.now())
                .build();
    }

    public static AuthResponse error(String message) {
        return AuthResponse.builder()
                .success(false)
                .message(message)
                .issuedAt(LocalDateTime.now())
                .build();
    }
}