package com.jaiswarsecurities.core.security;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || 
            authentication.getPrincipal().equals("anonymousUser")) {
            return Optional.of("SYSTEM");
        }

        if (authentication.getPrincipal() instanceof CustomUserPrincipal userPrincipal) {
            return Optional.of(userPrincipal.getUsername());
        }

        return Optional.of(authentication.getName());
    }
}
