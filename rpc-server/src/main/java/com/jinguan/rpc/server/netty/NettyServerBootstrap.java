package com.jinguan.rpc.server.netty;

import com.jinguan.rpc.api.HelloService;
import com.jinguan.rpc.server.impl.HelloServiceImpl;

/**
 * Netty Server Bootstrap - Phase 2
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class NettyServerBootstrap {
    
    public static void main(String[] args) {
        // Create Netty RPC server
        NettyRpcServer server = new NettyRpcServer(9000);
        
        // Register services
        HelloService helloService = new HelloServiceImpl();
        server.register(helloService);
        
        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        
        // Start server
        System.out.println("========================================");
        System.out.println("   JG-RPC Netty Server Starting...     ");
        System.out.println("   Phase 2: Industrial Components      ");
        System.out.println("========================================");
        server.start();
    }
}

