package com.jaiswarsecurities.core.repository;

import com.jaiswarsecurities.core.entity.Portfolio;
import com.jaiswarsecurities.core.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    List<Portfolio> findByUser(User user);
    
    Optional<Portfolio> findByUserAndSymbol(User user, String symbol);
    
    List<Portfolio> findByUserAndTotalQuantityGreaterThan(User user, Integer quantity);
    
    @Query("SELECT p FROM Portfolio p WHERE p.user = :user AND p.totalQuantity > 0")
    List<Portfolio> findActiveHoldingsByUser(@Param("user") User user);

    @Query("SELECT SUM(p.currentValue) FROM Portfolio p WHERE p.user = :user AND p.totalQuantity > 0")
    BigDecimal calculateTotalPortfolioValue(@Param("user") User user);

    @Query("SELECT SUM(p.investedAmount) FROM Portfolio p WHERE p.user = :user AND p.totalQuantity > 0")
    BigDecimal calculateTotalInvestedAmount(@Param("user") User user);

    @Query("SELECT SUM(p.unrealizedPnl) FROM Portfolio p WHERE p.user = :user AND p.totalQuantity > 0")
    BigDecimal calculateTotalUnrealizedPnl(@Param("user") User user);

    @Query("SELECT SUM(p.realizedPnl) FROM Portfolio p WHERE p.user = :user")
    BigDecimal calculateTotalRealizedPnl(@Param("user") User user);

    @Query("SELECT p FROM Portfolio p WHERE p.user = :user ORDER BY p.currentValue DESC")
    List<Portfolio> findByUserOrderByCurrentValueDesc(@Param("user") User user);

    @Query("SELECT COUNT(p) FROM Portfolio p WHERE p.user = :user AND p.totalQuantity > 0")
    long countActivePositions(@Param("user") User user);
}
