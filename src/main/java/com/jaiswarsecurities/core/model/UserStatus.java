package com.jaiswarsecurities.core.model;

/**
 * User account status
 */
public enum UserStatus {
    ACTIVE,               // Active account
    INACTIVE,             // Temporarily disabled
    SUSPENDED,            // Suspended by admin
    PENDING_VERIFICATION, // Email not verified
    EXPIRED,              // Account expired
    CREDENTIALS_EXPIRED   // Password expired
}