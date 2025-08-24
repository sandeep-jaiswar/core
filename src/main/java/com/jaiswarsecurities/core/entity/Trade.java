package com.jaiswarsecurities.core.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trades", indexes = {
    @Index(name = "idx_trade_user_id", columnList = "user_id"),
    @Index(name = "idx_trade_symbol", columnList = "symbol"),
    @Index(name = "idx_trade_date", columnList = "trade_date"),
    @Index(name = "idx_trade_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Size(max = 20)
    @Column(name = "symbol", nullable = false)
    private String symbol; // Stock symbol like RELIANCE, INFY, etc.

    @NotBlank
    @Size(max = 100)
    @Column(name = "company_name", nullable = false)
    private String companyName;

    @NotNull
    @Column(name = "trade_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TradeType tradeType; // BUY, SELL

    @NotNull
    @Column(name = "order_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderType orderType; // MARKET, LIMIT, STOP_LOSS

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "price", precision = 15, scale = 2, nullable = false)
    private BigDecimal price;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "total_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "brokerage", precision = 10, scale = 2)
    private BigDecimal brokerage;

    @Column(name = "taxes", precision = 10, scale = 2)
    private BigDecimal taxes;

    @Column(name = "net_amount", precision = 15, scale = 2)
    private BigDecimal netAmount;

    @NotNull
    @Column(name = "trade_date", nullable = false)
    private LocalDateTime tradeDate;

    @Column(name = "execution_date")
    private LocalDateTime executionDate;

    @NotNull
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TradeStatus status; // PENDING, EXECUTED, PARTIALLY_EXECUTED, CANCELLED, REJECTED

    @Size(max = 255)
    @Column(name = "notes")
    private String notes;

    @Column(name = "exchange", length = 10)
    private String exchange; // NSE, BSE

    @Column(name = "segment", length = 20)
    private String segment; // CASH, F&O, CURRENCY

    public enum TradeType {
        BUY, SELL
    }

    public enum OrderType {
        MARKET, LIMIT, STOP_LOSS, STOP_LOSS_MARKET
    }

    public enum TradeStatus {
        PENDING, EXECUTED, PARTIALLY_EXECUTED, CANCELLED, REJECTED, EXPIRED
    }
}
