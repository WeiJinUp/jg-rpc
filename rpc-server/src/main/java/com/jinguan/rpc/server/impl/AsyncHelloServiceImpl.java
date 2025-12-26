package com.jinguan.rpc.server.impl;

import com.jinguan.rpc.api.async.AsyncHelloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Async Hello Service Implementation
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class AsyncHelloServiceImpl implements AsyncHelloService {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncHelloServiceImpl.class);
    
    @Override
    public CompletableFuture<String> helloAsync(String name) {
        logger.info("Processing async request: helloAsync({})", name);
        
        return CompletableFuture.supplyAsync(() -> {
            String message = "Async Hello, " + name + "! (from AsyncHelloService)";
            logger.info("Async task completed: {}", message);
            return message;
        });
    }
    
    @Override
    public CompletableFuture<String> helloWithDelay(String name, long delayMs) {
        logger.info("Processing delayed async request: helloWithDelay({}, {}ms)", name, delayMs);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delayMs);
                String message = "Hello " + name + " after " + delayMs + "ms delay";
                logger.info("Delayed task completed: {}", message);
                return message;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Task interrupted", e);
            }
        });
    }
}

