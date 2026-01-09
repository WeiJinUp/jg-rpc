package com.jinguan.rpc.server;

import com.jinguan.rpc.api.HelloService;
import com.jinguan.rpc.api.async.AsyncHelloService;
import com.jinguan.rpc.server.impl.AsyncHelloServiceImpl;
import com.jinguan.rpc.server.impl.HelloServiceImpl;
import com.jinguan.rpc.server.registry.ZookeeperServiceRegistry;

/**
 * 第三阶段服务器启动类 - 演示生产级特性
 * Phase 3 Server Bootstrap
 * 
 * 该启动类演示了第三阶段的所有核心特性：
 * 1. 服务注册：自动注册到Zookeeper
 * 2. 负载均衡：客户端自动选择服务实例
 * 3. 异步调用：支持CompletableFuture异步RPC
 * 4. 优雅停机：JVM关闭时自动清理资源
 * 
 * 启动流程：
 * 1. 创建Zookeeper服务注册中心
 * 2. 创建带注册的RPC服务器
 * 3. 发布同步服务（HelloService）
 * 4. 发布异步服务（AsyncHelloService）
 * 5. 启动服务器
 * 
 * 使用说明：
 * - 确保Zookeeper已启动（默认localhost:2181）
 * - 启动后，服务会自动注册到Zookeeper
 * - 客户端可以通过服务发现找到该服务器
 * - 按Ctrl+C触发优雅停机
 * 
 * 测试建议：
 * - 启动多个服务器实例，测试负载均衡
 * - 测试异步RPC调用
 * - 测试优雅停机（观察日志）
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class Phase3ServerBootstrap {
    
    /**
     * Zookeeper连接地址
     */
    private static final String ZK_ADDRESS = "localhost:2181";
    
    /**
     * 服务器监听端口
     */
    private static final int SERVER_PORT = 9001;
    
    /**
     * 主方法 - 启动第三阶段RPC服务器
     * Main method
     * 
     * 启动步骤（详细）：
     * 1. 打印启动信息
     * 2. 创建Zookeeper服务注册中心
     * 3. 创建带注册的RPC服务器
     * 4. 创建并发布同步服务
     * 5. 创建并发布异步服务
     * 6. 启动服务器（阻塞）
     * 
     * 优雅停机：
     * - 按Ctrl+C触发JVM关闭钩子
     * - 自动执行shutdown()方法
     * - 注销服务 -> 等待请求完成 -> 关闭服务器
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 步骤1：打印启动信息
        System.out.println("========================================");
        System.out.println("   JG-RPC Server Phase 3               ");
        System.out.println("   Production-Grade Features           ");
        System.out.println("========================================");
        System.out.println();
        System.out.println("Features:");
        System.out.println("  ✓ Service Registry (Zookeeper)");
        System.out.println("  ✓ Load Balancing");
        System.out.println("  ✓ Async Calls");
        System.out.println("  ✓ Graceful Shutdown");
        System.out.println("========================================");
        System.out.println();
        
        // 步骤2：创建Zookeeper服务注册中心
        ZookeeperServiceRegistry registry = new ZookeeperServiceRegistry(ZK_ADDRESS);
        
        // 步骤3：创建带注册的RPC服务器
        // 服务器会自动注册JVM关闭钩子
        RpcServerWithRegistry server = new RpcServerWithRegistry(SERVER_PORT, registry);
        
        // 步骤4：发布同步服务
        HelloService helloService = new HelloServiceImpl();
        server.publishService(helloService);
        
        // 步骤5：发布异步服务
        AsyncHelloService asyncService = new AsyncHelloServiceImpl();
        server.publishService(asyncService);
        
        System.out.println("Services published:");
        System.out.println("  - HelloService");
        System.out.println("  - AsyncHelloService");
        System.out.println();
        System.out.println("Press Ctrl+C to trigger graceful shutdown...");
        System.out.println("========================================");
        System.out.println();
        
        // 步骤6：启动服务器（阻塞，直到关闭）
        server.start();
    }
}

