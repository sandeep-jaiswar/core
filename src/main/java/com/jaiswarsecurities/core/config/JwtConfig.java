package com.jaiswarsecurities.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtConfig {
    private String secret = "jaiswarSecurities2024TradingSystemVerySecretKeyThatShouldBeAtLeast256BitsLongForHS256Algorithm";
    private long accessTokenExpiration = 900000; // 15 minutes
    private long refreshTokenExpiration = 86400000; // 24 hours
    private String issuer = "Jaiswar Securities";
}
