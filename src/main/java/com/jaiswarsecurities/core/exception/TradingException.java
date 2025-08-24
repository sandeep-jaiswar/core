package com.jaiswarsecurities.core.exception;

public class TradingException extends BusinessException {
    
    private String symbol;
    private String tradeType;

    public TradingException(String message) {
        super(message);
    }

    public TradingException(String message, String symbol, String tradeType) {
        super(message);
        this.symbol = symbol;
        this.tradeType = tradeType;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getTradeType() {
        return tradeType;
    }
}
