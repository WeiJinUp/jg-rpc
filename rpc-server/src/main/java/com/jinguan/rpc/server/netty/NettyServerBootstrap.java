package com.jinguan.rpc.server.netty;

import com.jinguan.rpc.api.HelloService;
import com.jinguan.rpc.server.impl.HelloServiceImpl;

/**
 * Netty服务器启动类 - 第二阶段
 * Netty Server Bootstrap - Phase 2
 * 
 * 演示Netty服务器的启动流程
 * 
 * 第二阶段特性：
 * 1. 基于Netty的高性能服务器
 * 2. 自定义协议和编解码器
 * 3. 优雅关闭机制（ShutdownHook）
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class NettyServerBootstrap {
    
    /**
     * 主方法 - 启动Netty RPC服务器
     * 
     * 启动步骤：
     * 1. 创建Netty RPC服务器实例
     * 2. 注册服务实现
     * 3. 添加JVM关闭钩子，实现优雅关闭
     * 4. 启动服务器
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 步骤1：创建Netty RPC服务器，端口9000
        NettyRpcServer server = new NettyRpcServer(9000);
        
        // 步骤2：注册服务
        HelloService helloService = new HelloServiceImpl();
        server.register(helloService);
        
        // 步骤3：添加JVM关闭钩子
        // 当JVM关闭时（如Ctrl+C），自动调用server.shutdown()
        // 这保证了资源能够被正确释放
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        
        // 步骤4：启动服务器（阻塞）
        System.out.println("========================================");
        System.out.println("   JG-RPC Netty Server Starting...     ");
        System.out.println("   Phase 2: Industrial Components      ");
        System.out.println("========================================");
        server.start();
    }
}

