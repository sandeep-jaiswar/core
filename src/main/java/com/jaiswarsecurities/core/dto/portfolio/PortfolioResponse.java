package com.jaiswarsecurities.core.dto.portfolio;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioResponse {
    
    private List<HoldingResponse> holdings;
    private PortfolioSummary summary;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HoldingResponse {
        private Long id;
        private String symbol;
        private String companyName;
        private Integer totalQuantity;
        private Integer availableQuantity;
        private Integer blockedQuantity;
        private BigDecimal averagePrice;
        private BigDecimal investedAmount;
        private BigDecimal currentPrice;
        private BigDecimal currentValue;
        private BigDecimal unrealizedPnl;
        private BigDecimal unrealizedPnlPercent;
        private BigDecimal dayChange;
        private BigDecimal dayChangePercent;
        private String exchange;
        private String segment;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PortfolioSummary {
        private BigDecimal totalInvestedAmount;
        private BigDecimal totalCurrentValue;
        private BigDecimal totalUnrealizedPnl;
        private BigDecimal totalRealizedPnl;
        private BigDecimal totalPnl;
        private BigDecimal totalPnlPercent;
        private BigDecimal dayChange;
        private BigDecimal dayChangePercent;
        private long totalPositions;
        private long activePositions;
    }
}
