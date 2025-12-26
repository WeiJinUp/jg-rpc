package com.jinguan.rpc.api.async;

import java.util.concurrent.CompletableFuture;

/**
 * Async Hello Service - demonstrates asynchronous RPC calls
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public interface AsyncHelloService {
    
    /**
     * Async hello method that returns CompletableFuture
     * 
     * @param name name to greet
     * @return CompletableFuture with greeting message
     */
    CompletableFuture<String> helloAsync(String name);
    
    /**
     * Async method that simulates long-running task
     * 
     * @param name name
     * @param delayMs delay in milliseconds
     * @return CompletableFuture with result
     */
    CompletableFuture<String> helloWithDelay(String name, long delayMs);
}

