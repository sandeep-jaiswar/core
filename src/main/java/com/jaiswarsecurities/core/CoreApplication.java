package com.jaiswarsecurities.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableCaching
@EnableRetry
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
public class CoreApplication {

    public static void main(String[] args) {
        // Set system properties for high-performance trading
        System.setProperty("java.awt.headless", "true");
        System.setProperty("file.encoding", "UTF-8");
        
        SpringApplication.run(CoreApplication.class, args);
    }
}