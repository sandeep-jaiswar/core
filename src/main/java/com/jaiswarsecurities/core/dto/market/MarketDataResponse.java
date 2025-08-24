package com.jaiswarsecurities.core.dto.market;

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
public class MarketDataResponse {
    
    private String symbol;
    private String companyName;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal closePrice;
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal priceChange;
    private BigDecimal priceChangePercent;
    private Long volume;
    private BigDecimal bidPrice;
    private BigDecimal askPrice;
    private Integer bidQuantity;
    private Integer askQuantity;
    private LocalDateTime timestamp;
    private String exchange;
    private String segment;
    private BigDecimal marketCap;
}
