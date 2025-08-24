package com.jaiswarsecurities.core.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Repository
public class TradeDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void insertTrade(LocalDateTime timestamp, String symbol, double price, long quantity) {
        String sql = "INSERT INTO trades (timestamp, symbol, price, quantity) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, timestamp, symbol, price, quantity);
    }

    public void batchInsertTrades(List<Object[]> trades) {
        String sql = "INSERT INTO trades (timestamp, symbol, price, quantity) VALUES (?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Object[] trade = trades.get(i);
                ps.setObject(1, trade[0]); // timestamp
                ps.setString(2, (String) trade[1]); // symbol
                ps.setDouble(3, (Double) trade[2]); // price
                ps.setLong(4, (Long) trade[3]); // quantity
            }

            @Override
            public int getBatchSize() {
                return trades.size();
            }
        });
    }

    public CompletableFuture<Void> insertTradeAsync(LocalDateTime timestamp, String symbol, double price, long quantity) {
        return CompletableFuture.runAsync(() -> insertTrade(timestamp, symbol, price, quantity));
    }
}
