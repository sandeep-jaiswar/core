package com.jaiswarsecurities.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "portfolios", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "symbol"}),
       indexes = {
    @Index(name = "idx_portfolio_user_id", columnList = "user_id"),
    @Index(name = "idx_portfolio_symbol", columnList = "symbol")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Portfolio extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Size(max = 20)
    @Column(name = "symbol", nullable = false)
    private String symbol;

    @NotBlank
    @Size(max = 100)
    @Column(name = "company_name", nullable = false)
    private String companyName;

    @NotNull
    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "available_quantity")
    private Integer availableQuantity; // Quantity available for sale

    @Column(name = "blocked_quantity")
    private Integer blockedQuantity; // Quantity blocked in pending orders

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "average_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal averagePrice;

    @NotNull
    @DecimalMin(value = "0.0")
    @Column(name = "invested_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal investedAmount;

    @Column(name = "current_price", precision = 15, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "current_value", precision = 15, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "unrealized_pnl", precision = 15, scale = 2)
    private BigDecimal unrealizedPnl;

    @Column(name = "realized_pnl", precision = 15, scale = 2)
    private BigDecimal realizedPnl = BigDecimal.ZERO;

    @Column(name = "day_change", precision = 15, scale = 2)
    private BigDecimal dayChange;

    @Column(name = "day_change_percent", precision = 10, scale = 4)
    private BigDecimal dayChangePercent;

    @Column(name = "exchange", length = 10)
    private String exchange;

    @Column(name = "segment", length = 20)
    private String segment;

    // Calculated fields
    @Transient
    public BigDecimal getTotalPnl() {
        BigDecimal unrealized = unrealizedPnl != null ? unrealizedPnl : BigDecimal.ZERO;
        BigDecimal realized = realizedPnl != null ? realizedPnl : BigDecimal.ZERO;
        return unrealized.add(realized);
    }

    @Transient
    public BigDecimal getPnlPercent() {
        if (investedAmount != null && investedAmount.compareTo(BigDecimal.ZERO) > 0) {
            return getTotalPnl().divide(investedAmount, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        return BigDecimal.ZERO;
    }
}
