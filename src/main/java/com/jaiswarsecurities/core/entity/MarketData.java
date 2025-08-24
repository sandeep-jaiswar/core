package com.jaiswarsecurities.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_data", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"symbol", "timestamp"}),
       indexes = {
    @Index(name = "idx_market_symbol", columnList = "symbol"),
    @Index(name = "idx_market_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 20)
    @Column(name = "symbol", nullable = false)
    private String symbol;

    @NotBlank
    @Size(max = 100)
    @Column(name = "company_name", nullable = false)
    private String companyName;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "open_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal openPrice;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "high_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal highPrice;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "low_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal lowPrice;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "close_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal closePrice;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "current_price", precision = 15, scale = 2, nullable = false)
    private BigDecimal currentPrice;

    @Column(name = "previous_close", precision = 15, scale = 2)
    private BigDecimal previousClose;

    @Column(name = "price_change", precision = 15, scale = 2)
    private BigDecimal priceChange;

    @Column(name = "price_change_percent", precision = 10, scale = 4)
    private BigDecimal priceChangePercent;

    @NotNull
    @Column(name = "volume", nullable = false)
    private Long volume;

    @Column(name = "bid_price", precision = 15, scale = 2)
    private BigDecimal bidPrice;

    @Column(name = "ask_price", precision = 15, scale = 2)
    private BigDecimal askPrice;

    @Column(name = "bid_quantity")
    private Integer bidQuantity;

    @Column(name = "ask_quantity")
    private Integer askQuantity;

    @NotNull
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "exchange", length = 10)
    private String exchange;

    @Column(name = "segment", length = 20)
    private String segment;

    @Column(name = "market_cap", precision = 20, scale = 2)
    private BigDecimal marketCap;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
