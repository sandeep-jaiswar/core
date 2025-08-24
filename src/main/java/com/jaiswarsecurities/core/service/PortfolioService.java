package com.jaiswarsecurities.core.service;

import com.jaiswarsecurities.core.dto.portfolio.PortfolioResponse;
import com.jaiswarsecurities.core.entity.*;
import com.jaiswarsecurities.core.exception.ResourceNotFoundException;
import com.jaiswarsecurities.core.repository.PortfolioRepository;
import com.jaiswarsecurities.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final MarketDataService marketDataService;
    private final ModelMapper modelMapper;

    @Cacheable(value = "userPortfolio", key = "#username")
    @Transactional(readOnly = true)
    public PortfolioResponse getUserPortfolio(String username) {
        log.info("Retrieving portfolio for user: {}", username);

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        List<Portfolio> holdings = portfolioRepository.findActiveHoldingsByUser(user);
        
        // Update current prices
        updateCurrentPrices(holdings);

        List<PortfolioResponse.HoldingResponse> holdingResponses = holdings.stream()
            .map(this::mapToHoldingResponse)
            .collect(Collectors.toList());

        PortfolioResponse.PortfolioSummary summary = calculatePortfolioSummary(holdings);

        return PortfolioResponse.builder()
            .holdings(holdingResponses)
            .summary(summary)
            .build();
    }

    @Transactional(readOnly = true)
    public PortfolioResponse.PortfolioSummary getPortfolioSummary(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        List<Portfolio> holdings = portfolioRepository.findActiveHoldingsByUser(user);
        return calculatePortfolioSummary(holdings);
    }

    @Transactional
    @CacheEvict(value = {"userPortfolio", "userTrades"}, key = "#trade.user.username")
    public void updatePortfolioForTrade(Trade trade) {
        log.info("Updating portfolio for trade: {} - {} {} shares of {}", 
            trade.getId(), trade.getTradeType(), trade.getQuantity(), trade.getSymbol());

        Optional<Portfolio> existingPortfolio = portfolioRepository
            .findByUserAndSymbol(trade.getUser(), trade.getSymbol());

        if (existingPortfolio.isPresent()) {
            updateExistingPortfolio(existingPortfolio.get(), trade);
        } else {
            if (trade.getTradeType() == Trade.TradeType.BUY) {
                createNewPortfolio(trade);
            } else {
                log.warn("Attempting to sell {} shares of {} that user doesn't own", 
                    trade.getQuantity(), trade.getSymbol());
                throw new IllegalStateException("Cannot sell shares that are not owned");
            }
        }
    }

    @Async("portfolioUpdateExecutor")
    @CacheEvict(value = "userPortfolio", key = "#username")
    public void refreshPortfolioPrices(String username) {
        log.info("Refreshing portfolio prices for user: {}", username);

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        List<Portfolio> holdings = portfolioRepository.findActiveHoldingsByUser(user);
        updateCurrentPrices(holdings);

        // Save updated portfolios
        portfolioRepository.saveAll(holdings);
    }

    private void updateExistingPortfolio(Portfolio portfolio, Trade trade) {
        if (trade.getTradeType() == Trade.TradeType.BUY) {
            // Calculate new average price
            BigDecimal currentValue = portfolio.getAveragePrice()
                .multiply(new BigDecimal(portfolio.getTotalQuantity()));
            BigDecimal tradeValue = trade.getPrice()
                .multiply(new BigDecimal(trade.getQuantity()));
            
            int newQuantity = portfolio.getTotalQuantity() + trade.getQuantity();
            BigDecimal newAveragePrice = currentValue.add(tradeValue)
                .divide(new BigDecimal(newQuantity), 4, RoundingMode.HALF_UP);

            portfolio.setTotalQuantity(newQuantity);
            portfolio.setAvailableQuantity(portfolio.getAvailableQuantity() + trade.getQuantity());
            portfolio.setAveragePrice(newAveragePrice);
            portfolio.setInvestedAmount(portfolio.getInvestedAmount().add(trade.getTotalAmount()));
            
        } else { // SELL
            if (portfolio.getAvailableQuantity() < trade.getQuantity()) {
                throw new IllegalStateException("Insufficient shares to sell");
            }

            portfolio.setTotalQuantity(portfolio.getTotalQuantity() - trade.getQuantity());
            portfolio.setAvailableQuantity(portfolio.getAvailableQuantity() - trade.getQuantity());
            
            // Calculate realized P&L
            BigDecimal soldValue = trade.getTotalAmount();
            BigDecimal costBasis = portfolio.getAveragePrice()
                .multiply(new BigDecimal(trade.getQuantity()));
            BigDecimal realizedPnl = soldValue.subtract(costBasis);
            
            portfolio.setRealizedPnl(portfolio.getRealizedPnl().add(realizedPnl));
            portfolio.setInvestedAmount(portfolio.getInvestedAmount().subtract(costBasis));
        }

        portfolioRepository.save(portfolio);
        log.info("Updated portfolio for {}: {} shares", trade.getSymbol(), portfolio.getTotalQuantity());
    }

    private void createNewPortfolio(Trade trade) {
        Portfolio newPortfolio = Portfolio.builder()
            .user(trade.getUser())
            .symbol(trade.getSymbol())
            .companyName(trade.getCompanyName())
            .totalQuantity(trade.getQuantity())
            .availableQuantity(trade.getQuantity())
            .blockedQuantity(0)
            .averagePrice(trade.getPrice())
            .investedAmount(trade.getTotalAmount())
            .currentPrice(trade.getPrice())
            .currentValue(trade.getPrice().multiply(new BigDecimal(trade.getQuantity())))
            .unrealizedPnl(BigDecimal.ZERO)
            .realizedPnl(BigDecimal.ZERO)
            .dayChange(BigDecimal.ZERO)
            .dayChangePercent(BigDecimal.ZERO)
            .exchange(trade.getExchange())
            .segment(trade.getSegment())
            .build();

        portfolioRepository.save(newPortfolio);
        log.info("Created new portfolio for {}: {} shares", trade.getSymbol(), trade.getQuantity());
    }

    private void updateCurrentPrices(List<Portfolio> holdings) {
        for (Portfolio holding : holdings) {
            try {
                MarketData marketData = marketDataService.getMarketData(holding.getSymbol());
                
                holding.setCurrentPrice(marketData.getCurrentPrice());
                holding.setCurrentValue(marketData.getCurrentPrice()
                    .multiply(new BigDecimal(holding.getTotalQuantity())));
                
                // Calculate unrealized P&L
                BigDecimal unrealizedPnl = holding.getCurrentValue()
                    .subtract(holding.getInvestedAmount());
                holding.setUnrealizedPnl(unrealizedPnl);
                
                // Set day change
                holding.setDayChange(marketData.getPriceChange());
                holding.setDayChangePercent(marketData.getPriceChangePercent());
                
            } catch (Exception e) {
                log.warn("Failed to update price for {}: {}", holding.getSymbol(), e.getMessage());
            }
        }
    }

    private PortfolioResponse.PortfolioSummary calculatePortfolioSummary(List<Portfolio> holdings) {
        BigDecimal totalInvestedAmount = holdings.stream()
            .map(Portfolio::getInvestedAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCurrentValue = holdings.stream()
            .map(Portfolio::getCurrentValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalUnrealizedPnl = holdings.stream()
            .map(Portfolio::getUnrealizedPnl)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRealizedPnl = holdings.stream()
            .map(Portfolio::getRealizedPnl)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPnl = totalUnrealizedPnl.add(totalRealizedPnl);
        
        BigDecimal totalPnlPercent = totalInvestedAmount.compareTo(BigDecimal.ZERO) > 0 
            ? totalPnl.divide(totalInvestedAmount, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;

        BigDecimal dayChange = holdings.stream()
            .map(holding -> holding.getDayChange() != null 
                ? holding.getDayChange().multiply(new BigDecimal(holding.getTotalQuantity()))
                : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dayChangePercent = totalCurrentValue.compareTo(BigDecimal.ZERO) > 0
            ? dayChange.divide(totalCurrentValue.subtract(dayChange), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;

        return PortfolioResponse.PortfolioSummary.builder()
            .totalInvestedAmount(totalInvestedAmount)
            .totalCurrentValue(totalCurrentValue)
            .totalUnrealizedPnl(totalUnrealizedPnl)
            .totalRealizedPnl(totalRealizedPnl)
            .totalPnl(totalPnl)
            .totalPnlPercent(totalPnlPercent)
            .dayChange(dayChange)
            .dayChangePercent(dayChangePercent)
            .totalPositions(holdings.size())
            .activePositions(holdings.stream()
                .mapToLong(holding -> holding.getTotalQuantity() > 0 ? 1 : 0)
                .sum())
            .build();
    }

    private PortfolioResponse.HoldingResponse mapToHoldingResponse(Portfolio portfolio) {
        PortfolioResponse.HoldingResponse response = modelMapper
            .map(portfolio, PortfolioResponse.HoldingResponse.class);

        // Calculate unrealized P&L percentage
        if (portfolio.getInvestedAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal unrealizedPnlPercent = portfolio.getUnrealizedPnl()
                .divide(portfolio.getInvestedAmount(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
            response.setUnrealizedPnlPercent(unrealizedPnlPercent);
        } else {
            response.setUnrealizedPnlPercent(BigDecimal.ZERO);
        }

        return response;
    }
}
