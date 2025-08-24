package com.jaiswarsecurities.core.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends BusinessException {
    
    private final BigDecimal availableBalance;
    private final BigDecimal requiredAmount;

    public InsufficientBalanceException(BigDecimal availableBalance, BigDecimal requiredAmount) {
        super(String.format("Insufficient balance. Available: %s, Required: %s", 
              availableBalance, requiredAmount));
        this.availableBalance = availableBalance;
        this.requiredAmount = requiredAmount;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getRequiredAmount() {
        return requiredAmount;
    }
}
