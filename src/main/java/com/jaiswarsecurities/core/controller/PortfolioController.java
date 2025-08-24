package com.jaiswarsecurities.core.controller;

import com.jaiswarsecurities.core.dto.common.ApiResponse;
import com.jaiswarsecurities.core.dto.portfolio.PortfolioResponse;
import com.jaiswarsecurities.core.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Portfolio", description = "Portfolio management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    @Operation(summary = "Get user's portfolio", description = "Retrieve complete portfolio with holdings and summary")
    @PreAuthorize("hasRole('TRADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PortfolioResponse>> getPortfolio(
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        log.info("Retrieving portfolio for user: {}", authentication.getName());
        
        PortfolioResponse portfolio = portfolioService.getUserPortfolio(authentication.getName());
        
        ApiResponse<PortfolioResponse> response = ApiResponse.success(
            "Portfolio retrieved successfully",
            portfolio
        );
        response.setPath(httpRequest.getRequestURI());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get portfolio summary", description = "Get overall portfolio performance summary")
    @PreAuthorize("hasRole('TRADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PortfolioResponse.PortfolioSummary>> getPortfolioSummary(
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        PortfolioResponse.PortfolioSummary summary = portfolioService.getPortfolioSummary(authentication.getName());
        
        ApiResponse<PortfolioResponse.PortfolioSummary> response = ApiResponse.success(
            "Portfolio summary retrieved successfully",
            summary
        );
        response.setPath(httpRequest.getRequestURI());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh portfolio prices", description = "Update current market prices for all holdings")
    @PreAuthorize("hasRole('TRADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> refreshPortfolio(
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        log.info("Refreshing portfolio for user: {}", authentication.getName());
        
        portfolioService.refreshPortfolioPrices(authentication.getName());
        
        ApiResponse<String> response = ApiResponse.success(
            "Portfolio prices refreshed successfully",
            "Portfolio updated with latest market data"
        );
        response.setPath(httpRequest.getRequestURI());
        
        return ResponseEntity.ok(response);
    }
}
