package com.jaiswarsecurities.core.dto.trade;

import com.jaiswarsecurities.core.entity.Trade;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TradeRequest {
    
    @NotBlank(message = "Symbol is required")
    @Size(max = 20, message = "Symbol must not exceed 20 characters")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "Symbol must contain only uppercase letters and numbers")
    private String symbol;
    
    @NotBlank(message = "Company name is required")
    @Size(max = 100, message = "Company name must not exceed 100 characters")
    private String companyName;
    
    @NotNull(message = "Trade type is required")
    private Trade.TradeType tradeType;
    
    @NotNull(message = "Order type is required")
    private Trade.OrderType orderType;
    
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 10000, message = "Quantity cannot exceed 10,000")
    private Integer quantity;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Price must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Price cannot exceed 999,999.99")
    private BigDecimal price;
    
    @Size(max = 255, message = "Notes must not exceed 255 characters")
    private String notes;
    
    @Pattern(regexp = "^(NSE|BSE)$", message = "Exchange must be NSE or BSE")
    private String exchange = "NSE";
    
    @Pattern(regexp = "^(CASH|F&O|CURRENCY)$", message = "Segment must be CASH, F&O, or CURRENCY")
    private String segment = "CASH";
}
