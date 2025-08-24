package com.jaiswarsecurities.core.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class ClickHouseConfig {

    @Value("${spring.clickhouse.url}")
    private String url;

    @Value("${spring.clickhouse.username}")
    private String username;

    @Value("${spring.clickhouse.password}")
    private String password;

    @Value("${spring.clickhouse.driver-class-name}")
    private String driverClassName;

    @Bean
    public DataSource clickHouseDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        return new HikariDataSource(config);
    }

    @Bean
    public JdbcTemplate clickHouseJdbcTemplate() {
        return new JdbcTemplate(clickHouseDataSource());
    }
}
