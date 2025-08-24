package com.jaiswarsecurities.core.service;

import com.jaiswarsecurities.core.dto.trade.TradeRequest;
import com.jaiswarsecurities.core.dto.trade.TradeResponse;
import com.jaiswarsecurities.core.entity.Trade;
import com.jaiswarsecurities.core.entity.User;
import com.jaiswarsecurities.core.exception.BusinessException;
import com.jaiswarsecurities.core.exception.ResourceNotFoundException;
import com.jaiswarsecurities.core.repository.TradeRepository;
import com.jaiswarsecurities.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeService {

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final PortfolioService portfolioService;
    private final MarketDataService marketDataService;
    private final ModelMapper modelMapper;

    // Brokerage and tax rates (these would typically come from configuration)
    private static final BigDecimal BROKERAGE_RATE = new BigDecimal("0.0025"); // 0.25%
    private static final BigDecimal STT_RATE = new BigDecimal("0.001"); // 0.1%
    private static final BigDecimal GST_RATE = new BigDecimal("0.18"); // 18%

    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    @CacheEvict(value = {"userPortfolio", "userTrades"}, key = "#username")
    public TradeResponse placeTrade(String username, TradeRequest request) {
        log.info("Placing trade for user: {} - {} {} shares of {}", 
            username, request.getTradeType(), request.getQuantity(), request.getSymbol());

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        // Validate trading limits and account status
        validateTradeRequest(user, request);

        // Calculate trade amounts
        TradeAmounts amounts = calculateTradeAmounts(request);

        // Check if user has sufficient balance for buy orders
        if (request.getTradeType() == Trade.TradeType.BUY) {
            if (user.getAvailableBalance().compareTo(amounts.netAmount) < 0) {
                throw new BusinessException("Insufficient balance for this trade");
            }
        }

        // Create trade entity
        Trade trade = Trade.builder()
            .user(user)
            .symbol(request.getSymbol().toUpperCase())
            .companyName(request.getCompanyName())
            .tradeType(request.getTradeType())
            .orderType(request.getOrderType())
            .quantity(request.getQuantity())
            .price(request.getPrice())
            .totalAmount(amounts.totalAmount)
            .brokerage(amounts.brokerage)
            .taxes(amounts.taxes)
            .netAmount(amounts.netAmount)
            .tradeDate(LocalDateTime.now())
            .status(Trade.TradeStatus.PENDING)
            .notes(request.getNotes())
            .exchange(request.getExchange())
            .segment(request.getSegment())
            .build();

        Trade savedTrade = tradeRepository.save(trade);

        // Execute trade based on order type
        if (request.getOrderType() == Trade.OrderType.MARKET) {
            executeMarketOrder(savedTrade);
        }

        return mapToTradeResponse(savedTrade);
    }

    @Transactional
    public TradeResponse executeTrade(Long tradeId) {
        log.info("Executing trade: {}", tradeId);

        Trade trade = tradeRepository.findById(tradeId)
            .orElseThrow(() -> new ResourceNotFoundException("Trade not found: " + tradeId));

        if (trade.getStatus() != Trade.TradeStatus.PENDING) {
            throw new BusinessException("Trade cannot be executed, current status: " + trade.getStatus());
        }

        try {
            // Execute the trade
            trade.setStatus(Trade.TradeStatus.EXECUTED);
            trade.setExecutionDate(LocalDateTime.now());
            
            Trade executedTrade = tradeRepository.save(trade);

            // Update user's portfolio and balance
            updateUserBalanceAndPortfolio(executedTrade);

            log.info("Trade executed successfully: {}", tradeId);
            return mapToTradeResponse(executedTrade);

        } catch (Exception e) {
            trade.setStatus(Trade.TradeStatus.REJECTED);
            trade.setNotes("Execution failed: " + e.getMessage());
            tradeRepository.save(trade);
            throw new BusinessException("Trade execution failed: " + e.getMessage());
        }
    }

    @Transactional
    public TradeResponse cancelTrade(Long tradeId, String username) {
        log.info("Cancelling trade: {} for user: {}", tradeId, username);

        Trade trade = tradeRepository.findById(tradeId)
            .orElseThrow(() -> new ResourceNotFoundException("Trade not found: " + tradeId));

        if (!trade.getUser().getUsername().equals(username)) {
            throw new BusinessException("Unauthorized to cancel this trade");
        }

        if (trade.getStatus() != Trade.TradeStatus.PENDING) {
            throw new BusinessException("Trade cannot be cancelled, current status: " + trade.getStatus());
        }

        trade.setStatus(Trade.TradeStatus.CANCELLED);
        Trade cancelledTrade = tradeRepository.save(trade);

        return mapToTradeResponse(cancelledTrade);
    }

    @Cacheable(value = "userTrades", key = "#username")
    @Transactional(readOnly = true)
    public Page<TradeResponse> getUserTrades(String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        Page<Trade> trades = tradeRepository.findByUser(user, pageable);
        return trades.map(this::mapToTradeResponse);
    }

    @Transactional(readOnly = true)
    public TradeResponse getTradeById(Long tradeId, String username) {
        Trade trade = tradeRepository.findById(tradeId)
            .orElseThrow(() -> new ResourceNotFoundException("Trade not found: " + tradeId));

        if (!trade.getUser().getUsername().equals(username)) {
            throw new BusinessException("Unauthorized to access this trade");
        }

        return mapToTradeResponse(trade);
    }

    private void validateTradeRequest(User user, TradeRequest request) {
        if (!user.getEnabled()) {
            throw new BusinessException("Account is disabled");
        }

        if (user.getAccountLocked()) {
            throw new BusinessException("Account is locked");
        }

        if (user.getKycStatus() != User.KycStatus.APPROVED) {
            throw new BusinessException("KYC verification required");
        }

        // Validate market hours (simplified - in production, this would be more sophisticated)
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        if (hour < 9 || hour > 15) {
            if (request.getOrderType() == Trade.OrderType.MARKET) {
                throw new BusinessException("Market orders can only be placed during market hours (9 AM - 3:30 PM)");
            }
        }
    }

    private TradeAmounts calculateTradeAmounts(TradeRequest request) {
        BigDecimal totalAmount = request.getPrice()
            .multiply(new BigDecimal(request.getQuantity()))
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal brokerage = totalAmount
            .multiply(BROKERAGE_RATE)
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal stt = totalAmount
            .multiply(STT_RATE)
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal gst = brokerage
            .multiply(GST_RATE)
            .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalTaxes = stt.add(gst);

        BigDecimal netAmount = totalAmount.add(brokerage).add(totalTaxes);

        return new TradeAmounts(totalAmount, brokerage, totalTaxes, netAmount);
    }

    private void executeMarketOrder(Trade trade) {
        // In a real system, this would integrate with exchange APIs
        // For now, we'll simulate immediate execution
        trade.setStatus(Trade.TradeStatus.EXECUTED);
        trade.setExecutionDate(LocalDateTime.now());
        tradeRepository.save(trade);

        updateUserBalanceAndPortfolio(trade);
    }

    private void updateUserBalanceAndPortfolio(Trade trade) {
        User user = trade.getUser();
        
        if (trade.getTradeType() == Trade.TradeType.BUY) {
            // Deduct amount from user's balance
            user.setAvailableBalance(user.getAvailableBalance().subtract(trade.getNetAmount()));
        } else {
            // Add amount to user's balance (minus brokerage and taxes)
            user.setAvailableBalance(user.getAvailableBalance().add(trade.getTotalAmount().subtract(trade.getBrokerage()).subtract(trade.getTaxes())));
        }

        userRepository.save(user);

        // Update portfolio
        portfolioService.updatePortfolioForTrade(trade);
    }

    private TradeResponse mapToTradeResponse(Trade trade) {
        return modelMapper.map(trade, TradeResponse.class);
    }

    private static class TradeAmounts {
        final BigDecimal totalAmount;
        final BigDecimal brokerage;
        final BigDecimal taxes;
        final BigDecimal netAmount;

        TradeAmounts(BigDecimal totalAmount, BigDecimal brokerage, BigDecimal taxes, BigDecimal netAmount) {
            this.totalAmount = totalAmount;
            this.brokerage = brokerage;
            this.taxes = taxes;
            this.netAmount = netAmount;
        }
    }
}
