package com.jinguan.rpc.api.async;

import java.util.concurrent.CompletableFuture;

/**
 * 异步Hello服务接口 - 演示异步RPC调用
 * Async Hello Service - demonstrates asynchronous RPC calls
 * 
 * 该接口定义了返回CompletableFuture的异步方法
 * 
 * 异步RPC的特点：
 * 1. 方法返回CompletableFuture，不阻塞调用线程
 * 2. 服务端可以异步处理请求
 * 3. 客户端可以继续执行其他操作
 * 4. 通过Future获取结果
 * 
 * 使用场景：
 * - 长时间运行的任务
 * - 需要并发处理的场景
 * - 不阻塞主线程的场景
 * 
 * 客户端使用示例：
 * <pre>
 * CompletableFuture<String> future = helloService.helloAsync("World");
 * // 继续执行其他操作...
 * String result = future.get(); // 等待结果
 * </pre>
 * 
 * 服务端实现：
 * - 方法内部使用CompletableFuture.supplyAsync()异步执行
 * - 立即返回Future，不阻塞
 * - 后台线程处理完成后，Future自动完成
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public interface AsyncHelloService {
    
    /**
     * 异步Hello方法 - 返回CompletableFuture
     * Async hello method that returns CompletableFuture
     * 
     * 该方法立即返回Future，实际处理在后台线程中进行
     * 
     * 执行流程：
     * 1. 客户端调用方法，立即获得Future
     * 2. 服务端在后台线程处理请求
     * 3. 处理完成后，Future自动完成
     * 4. 客户端通过Future.get()获取结果
     * 
     * 优势：
     * - 不阻塞调用线程
     * - 可以并发处理多个请求
     * - 提高系统吞吐量
     * 
     * @param name 要问候的名字
     * @return CompletableFuture，包含问候消息
     */
    CompletableFuture<String> helloAsync(String name);
    
    /**
     * 带延迟的异步方法 - 模拟长时间运行的任务
     * Async method that simulates long-running task
     * 
     * 该方法模拟需要较长时间处理的任务
     * 
     * 使用场景：
     * - 数据库查询
     * - 外部API调用
     * - 复杂计算
     * - 文件处理
     * 
     * 优势：
     * - 客户端不需要等待
     * - 可以设置超时时间
     * - 可以取消任务
     * 
     * @param name 名字
     * @param delayMs 延迟时间（毫秒），模拟处理时间
     * @return CompletableFuture，包含处理结果
     */
    CompletableFuture<String> helloWithDelay(String name, long delayMs);
}

