package com.jinguan.rpc.server.impl;

import com.jinguan.rpc.api.async.AsyncHelloService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * 异步Hello服务实现 - 演示异步RPC的服务端实现
 * Async Hello Service Implementation
 * 
 * 该实现展示了如何在服务端实现异步RPC方法
 * 
 * 实现原理：
 * 1. 方法立即返回CompletableFuture
 * 2. 使用CompletableFuture.supplyAsync()在后台线程执行
 * 3. 实际处理完成后，Future自动完成
 * 4. 客户端通过Future获取结果
 * 
 * 与同步方法的区别：
 * - 同步方法：处理完成后才返回结果
 * - 异步方法：立即返回Future，处理在后台进行
 * 
 * 优势：
 * - 不阻塞Netty的I/O线程
 * - 提高并发处理能力
 * - 适合长时间运行的任务
 * 
 * 注意事项：
 * - 异常处理：在supplyAsync的lambda中处理异常
 * - 线程中断：正确处理InterruptedException
 * - 资源管理：确保后台线程正确释放资源
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class AsyncHelloServiceImpl implements AsyncHelloService {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncHelloServiceImpl.class);
    
    /**
     * 异步Hello方法实现
     * Async hello method implementation
     * 
     * 实现流程：
     * 1. 记录请求日志
     * 2. 使用supplyAsync()在ForkJoinPool中异步执行
     * 3. 立即返回Future，不阻塞调用线程
     * 4. 后台线程处理完成后，Future自动完成
     * 
     * supplyAsync()说明：
     * - 使用ForkJoinPool.commonPool()执行
     * - 适合CPU密集型任务
     * - 如果任务阻塞，建议使用自定义线程池
     * 
     * @param name 要问候的名字
     * @return CompletableFuture，包含问候消息
     */
    @Override
    public CompletableFuture<String> helloAsync(String name) {
        logger.info("Processing async request: helloAsync({})", name);
        
        // 使用supplyAsync在后台线程执行
        // 立即返回Future，不阻塞当前线程
        return CompletableFuture.supplyAsync(() -> {
            // 在后台线程中执行实际处理
            String message = "Async Hello, " + name + "! (from AsyncHelloService)";
            logger.info("Async task completed: {}", message);
            return message;
        });
    }
    
    /**
     * 带延迟的异步方法实现 - 模拟长时间运行的任务
     * Delayed async method implementation
     * 
     * 实现流程：
     * 1. 记录请求日志
     * 2. 使用supplyAsync()异步执行
     * 3. 在后台线程中休眠指定时间（模拟处理时间）
     * 4. 处理完成后返回结果
     * 
     * 异常处理：
     * - InterruptedException：正确设置中断标志
     * - 其他异常：包装为RuntimeException
     * 
     * 使用场景：
     * - 模拟数据库查询
     * - 模拟外部API调用
     * - 测试异步处理能力
     * 
     * @param name 名字
     * @param delayMs 延迟时间（毫秒）
     * @return CompletableFuture，包含处理结果
     */
    @Override
    public CompletableFuture<String> helloWithDelay(String name, long delayMs) {
        logger.info("Processing delayed async request: helloWithDelay({}, {}ms)", name, delayMs);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 模拟长时间处理（如数据库查询、外部API调用等）
                Thread.sleep(delayMs);
                
                String message = "Hello " + name + " after " + delayMs + "ms delay";
                logger.info("Delayed task completed: {}", message);
                return message;
                
            } catch (InterruptedException e) {
                // 正确处理线程中断
                Thread.currentThread().interrupt();
                throw new RuntimeException("Task interrupted", e);
            }
        });
    }
}

