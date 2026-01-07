package com.jinguan.rpc.server;

import com.jinguan.rpc.api.HelloService;
import com.jinguan.rpc.server.impl.HelloServiceImpl;

/**
 * 服务端启动类 - RPC服务器的主入口
 * Server Bootstrap - main entry point for starting the RPC server
 * 
 * 该类负责初始化RPC服务器，注册服务实现，并启动服务器监听客户端请求
 *
 * @author JinGuan
 * @version 1.0.0
 */
public class ServerBootstrap {
    
    /**
     * 主方法 - 启动RPC服务器
     * Main method - starts the RPC server
     * 
     * 启动流程：
     * 1. 创建RPC服务器实例，指定监听端口8888
     * 2. 创建服务实现类实例
     * 3. 将服务实现注册到RPC服务器
     * 4. 启动服务器，开始监听客户端连接
     * 
     * @param args 命令行参数（本示例未使用）
     */
    public static void main(String[] args) {
        // 步骤1：创建RPC服务器实例，监听8888端口
        // 该端口需要与客户端连接的端口保持一致
        RpcServer rpcServer = new RpcServer(8888);
        
        // 步骤2：创建服务实现类实例
        // HelloServiceImpl实现了HelloService接口，提供实际的业务逻辑
        HelloService helloService = new HelloServiceImpl();
        
        // 步骤3：注册服务到RPC服务器
        // 注册后，客户端就可以通过接口名调用该服务
        rpcServer.register(helloService);
        
        // 步骤4：打印启动横幅，启动服务器
        System.out.println("========================================");
        System.out.println("   JG-RPC Server Starting...           ");
        System.out.println("========================================");
        
        // 启动服务器，进入阻塞状态，持续监听客户端连接
        rpcServer.start();
    }
}

