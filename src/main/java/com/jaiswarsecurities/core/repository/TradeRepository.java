package com.jaiswarsecurities.core.repository;

import com.jaiswarsecurities.core.entity.Trade;
import com.jaiswarsecurities.core.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    Page<Trade> findByUser(User user, Pageable pageable);
    
    Page<Trade> findByUserAndSymbol(User user, String symbol, Pageable pageable);
    
    List<Trade> findByUserAndStatus(User user, Trade.TradeStatus status);
    
    List<Trade> findBySymbolAndStatus(String symbol, Trade.TradeStatus status);
    
    @Query("SELECT t FROM Trade t WHERE t.user = :user AND t.tradeDate BETWEEN :startDate AND :endDate")
    List<Trade> findByUserAndTradeDateBetween(
        @Param("user") User user, 
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT t FROM Trade t WHERE t.user = :user AND t.tradeType = :tradeType")
    List<Trade> findByUserAndTradeType(@Param("user") User user, @Param("tradeType") Trade.TradeType tradeType);

    @Query("SELECT SUM(t.totalAmount) FROM Trade t WHERE t.user = :user AND t.status = 'EXECUTED' AND t.tradeType = :tradeType")
    BigDecimal calculateTotalAmountByUserAndTradeType(
        @Param("user") User user, 
        @Param("tradeType") Trade.TradeType tradeType
    );

    @Query("SELECT COUNT(t) FROM Trade t WHERE t.user = :user AND t.status = 'EXECUTED' AND t.tradeDate >= :date")
    long countExecutedTradesAfterDate(@Param("user") User user, @Param("date") LocalDateTime date);

    @Query("SELECT t.symbol, COUNT(t), SUM(t.quantity), SUM(t.totalAmount) " +
           "FROM Trade t WHERE t.user = :user AND t.status = 'EXECUTED' " +
           "GROUP BY t.symbol ORDER BY SUM(t.totalAmount) DESC")
    List<Object[]> getTradingStatsBySymbol(@Param("user") User user);

    @Query("SELECT t FROM Trade t WHERE t.status IN :statuses ORDER BY t.tradeDate DESC")
    List<Trade> findByStatusInOrderByTradeDateDesc(@Param("statuses") List<Trade.TradeStatus> statuses);
}
