package com.jinguan.rpc.server;

import com.jinguan.rpc.api.registry.ServiceRegistry;
import com.jinguan.rpc.server.netty.NettyRpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * 带服务注册的RPC服务器 - 第三阶段增强版服务器
 * RPC Server with Service Registry
 * 
 * 该服务器是第二阶段的NettyRpcServer的增强版，新增功能：
 * 1. 自动服务注册：服务启动时自动注册到Zookeeper
 * 2. 优雅停机：服务关闭时自动注销，等待请求完成
 * 3. 服务发布：统一的服务发布接口
 * 
 * 与第二阶段服务器的区别：
 * - 第二阶段：只提供RPC服务，不注册到注册中心
 * - 第三阶段：自动注册到注册中心，客户端可以自动发现
 * - 第三阶段：支持优雅停机，保证数据不丢失
 * 
 * 工作流程：
 * 1. 创建服务器实例，获取本机IP地址
 * 2. 注册JVM关闭钩子
 * 3. 发布服务（本地注册 + 注册中心注册）
 * 4. 启动服务器
 * 5. 关闭时：注销服务 -> 等待请求完成 -> 关闭服务器
 * 
 * 优雅停机流程：
 * 1. 从注册中心注销所有服务（停止接收新请求）
 * 2. 等待正在处理的请求完成（5秒）
 * 3. 关闭Netty服务器
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class RpcServerWithRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcServerWithRegistry.class);
    
    /**
     * Netty RPC服务器 - 处理实际的RPC请求
     */
    private final NettyRpcServer nettyServer;
    
    /**
     * 服务注册中心 - 注册服务到Zookeeper
     */
    private final ServiceRegistry serviceRegistry;
    
    /**
     * 服务器地址 - 本机的IP和端口
     */
    private final InetSocketAddress serverAddress;
    
    /**
     * 已注册的服务列表 - 用于优雅关闭时清理
     * List of registered services for cleanup
     */
    private final List<String> registeredServices = new ArrayList<>();
    
    /**
     * 构造函数 - 创建带服务注册的RPC服务器
     * Constructor
     * 
     * 初始化流程：
     * 1. 创建NettyRpcServer实例
     * 2. 保存ServiceRegistry引用
     * 3. 获取本机IP地址
     * 4. 构建服务器地址（IP:Port）
     * 5. 注册JVM关闭钩子
     * 
     * 为什么需要获取本机IP？
     * - 注册到Zookeeper时需要提供可访问的地址
     * - 客户端会使用该地址连接服务器
     * - 不能使用localhost，因为客户端可能在其他机器
     * 
     * @param port 服务器监听端口
     * @param serviceRegistry 服务注册中心实例
     */
    public RpcServerWithRegistry(int port, ServiceRegistry serviceRegistry) {
        this.nettyServer = new NettyRpcServer(port);
        this.serviceRegistry = serviceRegistry;
        
        try {
            // 获取本机IP地址
            String host = InetAddress.getLocalHost().getHostAddress();
            this.serverAddress = new InetSocketAddress(host, port);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Failed to get local host address", e);
        }
        
        // 注册JVM关闭钩子，实现优雅停机
        addShutdownHook();
    }
    
    /**
     * 发布服务 - 同时注册到本地和注册中心
     * Register and publish a service
     * 
     * 发布流程：
     * 1. 注册到本地服务器（NettyRpcServer）
     * 2. 获取服务实现的所有接口
     * 3. 遍历接口，逐个注册到Zookeeper
     * 4. 记录已注册的服务名
     * 
     * 为什么需要本地注册？
     * - NettyRpcServer需要知道哪些服务可用
     * - 当收到RPC请求时，需要调用对应的服务实现
     * 
     * 为什么需要注册中心注册？
     * - 客户端需要从注册中心发现服务
     * - 支持服务的高可用和负载均衡
     * 
     * @param service 服务实现实例
     */
    public void publishService(Object service) {
        // 步骤1：注册到本地服务器
        nettyServer.register(service);
        
        // 步骤2-3：注册到Zookeeper
        Class<?>[] interfaces = service.getClass().getInterfaces();
        for (Class<?> intf : interfaces) {
            String serviceName = intf.getName();
            // 注册到注册中心
            serviceRegistry.register(serviceName, serverAddress);
            // 记录已注册的服务
            registeredServices.add(serviceName);
            logger.info("Service published to registry: {} -> {}", serviceName, serverAddress);
        }
    }
    
    /**
     * 启动服务器
     * Start the server
     * 
     * 启动后，服务器开始监听客户端连接
     * 已注册的服务可以被客户端发现和调用
     */
    public void start() {
        logger.info("Starting RPC server with service registry...");
        nettyServer.start();
    }
    
    /**
     * 优雅停机 - 分步骤关闭服务器
     * Graceful shutdown
     * 
     * 优雅停机流程（3个步骤）：
     * 1. 从注册中心注销所有服务
     *    - 停止接收新的客户端请求
     *    - 客户端发现服务不可用，不再发送请求
     * 2. 等待正在处理的请求完成
     *    - 给正在处理的请求一些时间完成
     *    - 避免数据丢失
     * 3. 关闭Netty服务器
     *    - 关闭所有连接
     *    - 释放资源
     * 
     * 为什么需要等待？
     * - 正在处理的请求需要时间完成
     * - 立即关闭可能导致数据丢失
     * - 5秒是经验值，可以根据实际情况调整
     * 
     * 为什么先注销再等待？
     * - 先停止接收新请求
     * - 再等待现有请求完成
     * - 避免新请求和关闭操作冲突
     */
    public void shutdown() {
        logger.info("========================================");
        logger.info("  Starting graceful shutdown...");
        logger.info("========================================");
        
        // 步骤1：从注册中心注销所有服务
        logger.info("Step 1: Unregistering services from Zookeeper...");
        try {
            serviceRegistry.unregisterAll(serverAddress);
            logger.info("✓ All services unregistered");
        } catch (Exception e) {
            // 即使注销失败，也要继续关闭流程
            logger.error("Error unregistering services", e);
        }
        
        // 步骤2：等待正在处理的请求完成
        logger.info("Step 2: Waiting for ongoing requests to complete...");
        try {
            Thread.sleep(5000); // 等待5秒
            logger.info("✓ Wait completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 步骤3：关闭Netty服务器
        logger.info("Step 3: Shutting down Netty server...");
        nettyServer.shutdown();
        logger.info("✓ Netty server stopped");
        
        logger.info("========================================");
        logger.info("  Graceful shutdown completed");
        logger.info("========================================");
    }
    
    /**
     * 添加JVM关闭钩子 - 实现自动优雅停机
     * Add JVM shutdown hook
     * 
     * 关闭钩子的作用：
     * - 当JVM关闭时（如Ctrl+C、kill命令），自动调用shutdown()
     * - 确保服务能够优雅关闭，不丢失数据
     * - 不需要手动调用shutdown()
     * 
     * 触发场景：
     * - 用户按Ctrl+C
     * - 执行kill命令（非kill -9）
     * - 系统关闭
     * - 应用程序正常退出
     * 
     * 注意：kill -9不会触发关闭钩子
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered");
            shutdown();
        }, "shutdown-hook"));
    }
}

