package com.jinguan.rpc.server;

import com.jinguan.rpc.api.HelloService;
import com.jinguan.rpc.server.impl.HelloServiceImpl;

/**
 * Server Bootstrap - main entry point for starting the RPC server
 *
 * @author JinGuan
 * @version 1.0.0
 */
public class ServerBootstrap {
    
    public static void main(String[] args) {
        // Create RPC server instance
        RpcServer rpcServer = new RpcServer(8888);
        
        // Register services
        HelloService helloService = new HelloServiceImpl();
        rpcServer.register(helloService);
        
        // Start server
        System.out.println("========================================");
        System.out.println("   JG-RPC Server Starting...           ");
        System.out.println("========================================");
        rpcServer.start();
    }
}

