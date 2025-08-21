package com.jaiswarsecurities.core.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.jaiswarsecurities.core.dto.auth.AuthResponse;
import com.jaiswarsecurities.core.service.JwtService;

import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for the application
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Validation failed");
        response.put("errors", errors);
        response.put("timestamp", LocalDateTime.now());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle user already exists exception
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<AuthResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.warn("User already exists: {}", ex.getMessage());

        AuthResponse response = AuthResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .issuedAt(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    /**
     * Handle user not found exception
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<AuthResponse> handleUserNotFoundException(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());

        AuthResponse response = AuthResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .issuedAt(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle authentication exception
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<AuthResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication error: {}", ex.getMessage());

        AuthResponse response = AuthResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .issuedAt(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle bad credentials exception
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<AuthResponse> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("Bad credentials: {}", ex.getMessage());

        AuthResponse response = AuthResponse.builder()
                .success(false)
                .message("Invalid username or password")
                .issuedAt(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handle disabled account exception
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<AuthResponse> handleDisabledException(DisabledException ex) {
        log.warn("Account disabled: {}", ex.getMessage());

        AuthResponse response = AuthResponse.builder()
                .success(false)
                .message("Account is disabled. Please contact support.")
                .issuedAt(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    /**
     * Handle locked account exception
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<AuthResponse> handleLockedException(LockedException ex) {
        log.warn("Account locked: {}", ex.getMessage());

        AuthResponse response = AuthResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .issuedAt(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.LOCKED);
    }

    /**
     * Handle general exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthResponse> handleGeneralException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        AuthResponse response = AuthResponse.builder()
                .success(false)
                .message("An unexpected error occurred. Please try again later.")
                .issuedAt(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}