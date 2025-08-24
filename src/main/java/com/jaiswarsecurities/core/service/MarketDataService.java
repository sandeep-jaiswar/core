package com.jaiswarsecurities.core.service;

import com.jaiswarsecurities.core.dto.market.MarketDataResponse;
import com.jaiswarsecurities.core.entity.MarketData;
import com.jaiswarsecurities.core.exception.ResourceNotFoundException;
import com.jaiswarsecurities.core.repository.MarketDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataService {

    private final MarketDataRepository marketDataRepository;
    private final ModelMapper modelMapper;
    private final Random random = new Random();

    @Cacheable(value = "marketData", key = "#symbol")
    @Transactional(readOnly = true)
    public MarketData getMarketData(String symbol) {
        return marketDataRepository.findBySymbol(symbol.toUpperCase())
            .orElseThrow(() -> new ResourceNotFoundException("Market data not found for symbol: " + symbol));
    }

    @Transactional(readOnly = true)
    public MarketDataResponse getMarketDataResponse(String symbol) {
        MarketData marketData = getMarketData(symbol);
        return modelMapper.map(marketData, MarketDataResponse.class);
    }

    @Transactional(readOnly = true)
    public List<MarketDataResponse> getAllActiveStocks() {
        List<MarketData> activeStocks = marketDataRepository.findActiveStocksOrderByVolume();
        return activeStocks.stream()
            .map(data -> modelMapper.map(data, MarketDataResponse.class))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MarketDataResponse> getTopGainers(int limit) {
        BigDecimal threshold = BigDecimal.ZERO;
        List<MarketData> gainers = marketDataRepository.findGainersByThreshold(threshold);
        
        return gainers.stream()
            .limit(limit)
            .map(data -> modelMapper.map(data, MarketDataResponse.class))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MarketDataResponse> getTopLosers(int limit) {
        BigDecimal threshold = BigDecimal.ZERO;
        List<MarketData> losers = marketDataRepository.findLosersByThreshold(threshold);
        
        return losers.stream()
            .limit(limit)
            .map(data -> modelMapper.map(data, MarketDataResponse.class))
            .collect(Collectors.toList());
    }

    // Scheduled task to update market data (runs every minute during market hours)
    @Scheduled(fixedRate = 60000) // 1 minute
    @Async("marketDataExecutor")
    @Transactional
    public void updateMarketData() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        
        // Only update during market hours (9 AM - 3:30 PM)
        if (hour >= 9 && hour < 16) {
            log.info("Updating market data at: {}", now);
            
            List<String> activeSymbols = marketDataRepository.findAllActiveSymbols();
            
            for (String symbol : activeSymbols) {
                try {
                    updateSymbolData(symbol);
                } catch (Exception e) {
                    log.error("Failed to update market data for {}: {}", symbol, e.getMessage());
                }
            }
        }
    }

    @Transactional
    public void updateSymbolData(String symbol) {
        MarketData marketData = marketDataRepository.findBySymbol(symbol)
            .orElse(createNewMarketData(symbol));

        // Simulate market data updates (in production, this would fetch from exchange APIs)
        updatePricesSimulation(marketData);
        
        marketData.setTimestamp(LocalDateTime.now());
        marketDataRepository.save(marketData);
    }

    private MarketData createNewMarketData(String symbol) {
        log.info("Creating new market data for symbol: {}", symbol);
        
        // This is a simulation - in production, you'd fetch real data
        BigDecimal basePrice = new BigDecimal("1000.00");
        
        return MarketData.builder()
            .symbol(symbol.toUpperCase())
            .companyName(getCompanyName(symbol))
            .openPrice(basePrice)
            .highPrice(basePrice.multiply(new BigDecimal("1.02")))
            .lowPrice(basePrice.multiply(new BigDecimal("0.98")))
            .closePrice(basePrice)
            .currentPrice(basePrice)
            .previousClose(basePrice)
            .priceChange(BigDecimal.ZERO)
            .priceChangePercent(BigDecimal.ZERO)
            .volume(1000000L)
            .bidPrice(basePrice.subtract(new BigDecimal("0.50")))
            .askPrice(basePrice.add(new BigDecimal("0.50")))
            .bidQuantity(100)
            .askQuantity(100)
            .timestamp(LocalDateTime.now())
            .exchange("NSE")
            .segment("CASH")
            .isActive(true)
            .build();
    }

    private void updatePricesSimulation(MarketData marketData) {
        // Simulate price movements (±2%)
        double changePercent = (random.nextGaussian() * 0.5); // Standard deviation of 0.5%
        changePercent = Math.max(-2.0, Math.min(2.0, changePercent)); // Limit to ±2%
        
        BigDecimal previousPrice = marketData.getCurrentPrice();
        BigDecimal priceChange = previousPrice.multiply(new BigDecimal(changePercent / 100))
            .setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal newPrice = previousPrice.add(priceChange)
            .setScale(2, RoundingMode.HALF_UP);
        
        // Ensure price doesn't go negative
        if (newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            newPrice = previousPrice.multiply(new BigDecimal("0.99"));
        }
        
        marketData.setCurrentPrice(newPrice);
        marketData.setPriceChange(priceChange);
        
        if (marketData.getPreviousClose().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal changePercFromPrevClose = priceChange
                .divide(marketData.getPreviousClose(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            marketData.setPriceChangePercent(changePercFromPrevClose);
        }
        
        // Update high/low
        if (newPrice.compareTo(marketData.getHighPrice()) > 0) {
            marketData.setHighPrice(newPrice);
        }
        if (newPrice.compareTo(marketData.getLowPrice()) < 0) {
            marketData.setLowPrice(newPrice);
        }
        
        // Simulate volume
        long volumeChange = (long) (random.nextGaussian() * 10000);
        long newVolume = Math.max(1000, marketData.getVolume() + volumeChange);
        marketData.setVolume(newVolume);
        
        // Update bid/ask
        marketData.setBidPrice(newPrice.subtract(new BigDecimal("0.50")));
        marketData.setAskPrice(newPrice.add(new BigDecimal("0.50")));
    }

    private String getCompanyName(String symbol) {
        // This is a simple mapping - in production, you'd have a complete database
        return switch (symbol.toUpperCase()) {
            case "RELIANCE" -> "Reliance Industries Ltd";
            case "INFY" -> "Infosys Ltd";
            case "TCS" -> "Tata Consultancy Services Ltd";
            case "HDFCBANK" -> "HDFC Bank Ltd";
            case "ICICIBANK" -> "ICICI Bank Ltd";
            case "SBIN" -> "State Bank of India";
            case "ITC" -> "ITC Ltd";
            case "LT" -> "Larsen & Toubro Ltd";
            case "HCLTECH" -> "HCL Technologies Ltd";
            case "WIPRO" -> "Wipro Ltd";
            default -> symbol + " Company";
        };
    }

    @Transactional
    public void initializeMarketData() {
        log.info("Initializing market data for popular Indian stocks");
        
        String[] popularStocks = {
            "RELIANCE", "INFY", "TCS", "HDFCBANK", "ICICIBANK", 
            "SBIN", "ITC", "LT", "HCLTECH", "WIPRO"
        };
        
        for (String symbol : popularStocks) {
            if (!marketDataRepository.findBySymbol(symbol).isPresent()) {
                MarketData marketData = createNewMarketData(symbol);
                marketDataRepository.save(marketData);
                log.info("Initialized market data for: {}", symbol);
            }
        }
    }
}
