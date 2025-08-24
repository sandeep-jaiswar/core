package com.jaiswarsecurities.core.controller;

import com.jaiswarsecurities.core.dto.auth.*;
import com.jaiswarsecurities.core.dto.common.ApiResponse;
import com.jaiswarsecurities.core.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Login with username/email and password")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "Authentication successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", 
            description = "Invalid credentials"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "423", 
            description = "Account locked due to multiple failed attempts"
        )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody AuthRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Login attempt for user: {}", request.getUsernameOrEmail());
        
        AuthResponse authResponse = authService.authenticate(request);
        
        ApiResponse<AuthResponse> response = ApiResponse.success(
            "Authentication successful",
            authResponse
        );
        response.setPath(httpRequest.getRequestURI());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Create a new trading account")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", 
            description = "Registration successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "Validation error or user already exists"
        )
    })
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Registration attempt for username: {}", request.getUsername());
        
        AuthResponse authResponse = authService.register(request);
        
        ApiResponse<AuthResponse> response = ApiResponse.success(
            "Registration successful. Please check your email for verification.",
            authResponse
        );
        response.setPath(httpRequest.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Generate new access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        
        AuthResponse authResponse = authService.refreshToken(request);
        
        ApiResponse<AuthResponse> response = ApiResponse.success(
            "Token refreshed successfully",
            authResponse
        );
        response.setPath(httpRequest.getRequestURI());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidate user session")
    public ResponseEntity<ApiResponse<Void>> logout(
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        if (authentication != null) {
            authService.logout(authentication.getName());
            log.info("User logged out: {}", authentication.getName());
        }
        
        ApiResponse<Void> response = ApiResponse.success("Logout successful", null);
        response.setPath(httpRequest.getRequestURI());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", description = "Retrieve authenticated user's profile information")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getProfile(Authentication authentication) {
        
        // This would typically call a service to get full user details
        // For now, we'll return basic info from the authentication object
        String username = authentication.getName();
        
        ApiResponse<AuthResponse.UserInfo> response = ApiResponse.success(
            "Profile retrieved successfully",
            null // Would implement getUserProfile service method
        );
        
        return ResponseEntity.ok(response);
    }
}
