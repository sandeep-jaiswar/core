package com.jaiswarsecurities.core.service;

import com.jaiswarsecurities.core.dao.TradeDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class TradeService {

    @Autowired
    private TradeDao tradeDao;

    public void insertTrade(LocalDateTime timestamp, String symbol, double price, long quantity) {
        tradeDao.insertTrade(timestamp, symbol, price, quantity);
    }

    public void batchInsertTrades(List<Object[]> trades) {
        tradeDao.batchInsertTrades(trades);
    }

    public CompletableFuture<Void> insertTradeAsync(LocalDateTime timestamp, String symbol, double price, long quantity) {
        return tradeDao.insertTradeAsync(timestamp, symbol, price, quantity);
    }

}
