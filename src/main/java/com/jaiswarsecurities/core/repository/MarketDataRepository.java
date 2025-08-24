package com.jaiswarsecurities.core.repository;

import com.jaiswarsecurities.core.entity.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {

    Optional<MarketData> findBySymbol(String symbol);
    
    List<MarketData> findBySymbolIn(List<String> symbols);
    
    @Query("SELECT m FROM MarketData m WHERE m.symbol = :symbol AND m.timestamp BETWEEN :startTime AND :endTime ORDER BY m.timestamp")
    List<MarketData> findBySymbolAndTimestampBetween(
        @Param("symbol") String symbol,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    @Query("SELECT m FROM MarketData m WHERE m.symbol = :symbol ORDER BY m.timestamp DESC")
    List<MarketData> findBySymbolOrderByTimestampDesc(@Param("symbol") String symbol);

    @Query("SELECT m FROM MarketData m WHERE m.isActive = true ORDER BY m.volume DESC")
    List<MarketData> findActiveStocksOrderByVolume();

    @Query("SELECT m FROM MarketData m WHERE m.priceChangePercent > :threshold ORDER BY m.priceChangePercent DESC")
    List<MarketData> findGainersByThreshold(@Param("threshold") java.math.BigDecimal threshold);

    @Query("SELECT m FROM MarketData m WHERE m.priceChangePercent < :threshold ORDER BY m.priceChangePercent ASC")
    List<MarketData> findLosersByThreshold(@Param("threshold") java.math.BigDecimal threshold);

    @Query("SELECT m FROM MarketData m WHERE m.volume > :volumeThreshold ORDER BY m.volume DESC")
    List<MarketData> findHighVolumeStocks(@Param("volumeThreshold") Long volumeThreshold);

    @Query("SELECT DISTINCT m.symbol FROM MarketData m WHERE m.isActive = true ORDER BY m.symbol")
    List<String> findAllActiveSymbols();
}
