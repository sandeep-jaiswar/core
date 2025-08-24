package com.jaiswarsecurities.core.controller;

import com.jaiswarsecurities.core.dto.common.ApiResponse;
import com.jaiswarsecurities.core.dto.common.PageResponse;
import com.jaiswarsecurities.core.dto.trade.TradeRequest;
import com.jaiswarsecurities.core.dto.trade.TradeResponse;
import com.jaiswarsecurities.core.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Trading", description = "Trade execution and management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class TradeController {

    private final TradeService tradeService;

    @PostMapping
    @Operation(summary = "Place a new trade", description = "Submit a buy or sell order")
    @PreAuthorize("hasRole('TRADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TradeResponse>> placeTrade(
            @Valid @RequestBody TradeRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        log.info("Placing trade for user: {} - {} {} shares of {}", 
            authentication.getName(), request.getTradeType(), 
            request.getQuantity(), request.getSymbol());
        
        TradeResponse tradeResponse = tradeService.placeTrade(authentication.getName(), request);
        
        ApiResponse<TradeResponse> response = ApiResponse.success(
            "Trade placed successfully",
            tradeResponse
        );
        response.setPath(httpRequest.getRequestURI());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get user's trades", description = "Retrieve paginated list of user's trades")
    @PreAuthorize("hasRole('TRADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<TradeResponse>>> getTrades(
            @Parameter(description = "Page number (0-based)") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") 
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort direction (asc/desc)") 
            @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Sort field") 
            @RequestParam(defaultValue = "tradeDate") String sortBy,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        Sort sort = Sort.by(Sort.Direction.fromString(sortDir), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<TradeResponse> trades = tradeService.getUserTrades(authentication.getName(), pageable);
        
        PageResponse<TradeResponse> pageResponse = PageResponse.<TradeResponse>builder()
            .content(trades.getContent())
            .page(trades.getNumber())
            .size(trades.getSize())
            .totalElements(trades.getTotalElements())
            .totalPages(trades.getTotalPages())
            .first(trades.isFirst())
            .last(trades.isLast())
            .empty(trades.isEmpty())
            .build();
        
        ApiResponse<PageResponse<TradeResponse>> response = ApiResponse.success(
            "Trades retrieved successfully",
            pageResponse
        );
        response.setPath(httpRequest.getRequestURI());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{tradeId}")
    @Operation(summary = "Get trade by ID", description = "Retrieve specific trade details")
    @PreAuthorize("hasRole('TRADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TradeResponse>> getTradeById(
            @PathVariable Long tradeId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        TradeResponse trade = tradeService.getTradeById(tradeId, authentication.getName());
        
        ApiResponse<TradeResponse> response = ApiResponse.success(
            "Trade retrieved successfully",
            trade
        );
        response.setPath(httpRequest.getRequestURI());
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{tradeId}/execute")
    @Operation(summary = "Execute a pending trade", description = "Execute a trade that is in PENDING status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TradeResponse>> executeTrade(
            @PathVariable Long tradeId,
            HttpServletRequest httpRequest) {
        
        log.info("Executing trade: {}", tradeId);
        
        TradeResponse trade = tradeService.executeTrade(tradeId);
        
        ApiResponse<TradeResponse> response = ApiResponse.success(
            "Trade executed successfully",
            trade
        );
        response.setPath(httpRequest.getRequestURI());
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{tradeId}/cancel")
    @Operation(summary = "Cancel a pending trade", description = "Cancel a trade that is in PENDING status")
    @PreAuthorize("hasRole('TRADER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TradeResponse>> cancelTrade(
            @PathVariable Long tradeId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        
        log.info("Cancelling trade: {} for user: {}", tradeId, authentication.getName());
        
        TradeResponse trade = tradeService.cancelTrade(tradeId, authentication.getName());
        
        ApiResponse<TradeResponse> response = ApiResponse.success(
            "Trade cancelled successfully",
            trade
        );
        response.setPath(httpRequest.getRequestURI());
        
        return ResponseEntity.ok(response);
    }
}
