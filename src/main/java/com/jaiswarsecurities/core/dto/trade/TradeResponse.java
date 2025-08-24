package com.jaiswarsecurities.core.dto.trade;

import com.jaiswarsecurities.core.entity.Trade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeResponse {
    
    private Long id;
    private String symbol;
    private String companyName;
    private Trade.TradeType tradeType;
    private Trade.OrderType orderType;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private BigDecimal brokerage;
    private BigDecimal taxes;
    private BigDecimal netAmount;
    private LocalDateTime tradeDate;
    private LocalDateTime executionDate;
    private Trade.TradeStatus status;
    private String notes;
    private String exchange;
    private String segment;
}
