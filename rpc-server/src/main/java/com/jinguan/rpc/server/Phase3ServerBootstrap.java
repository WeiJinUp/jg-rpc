package com.jinguan.rpc.server;

import com.jinguan.rpc.api.HelloService;
import com.jinguan.rpc.api.async.AsyncHelloService;
import com.jinguan.rpc.server.impl.AsyncHelloServiceImpl;
import com.jinguan.rpc.server.impl.HelloServiceImpl;
import com.jinguan.rpc.server.registry.ZookeeperServiceRegistry;

/**
 * Phase 3 Server Bootstrap
 * 
 * Demonstrates service registry, load balancing, async calls, and graceful shutdown
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class Phase3ServerBootstrap {
    
    private static final String ZK_ADDRESS = "localhost:2181";
    private static final int SERVER_PORT = 9001;
    
    public static void main(String[] args) {
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
        
        // Create service registry
        ZookeeperServiceRegistry registry = new ZookeeperServiceRegistry(ZK_ADDRESS);
        
        // Create server with registry
        RpcServerWithRegistry server = new RpcServerWithRegistry(SERVER_PORT, registry);
        
        // Publish services
        HelloService helloService = new HelloServiceImpl();
        server.publishService(helloService);
        
        AsyncHelloService asyncService = new AsyncHelloServiceImpl();
        server.publishService(asyncService);
        
        System.out.println("Services published:");
        System.out.println("  - HelloService");
        System.out.println("  - AsyncHelloService");
        System.out.println();
        System.out.println("Press Ctrl+C to trigger graceful shutdown...");
        System.out.println("========================================");
        System.out.println();
        
        // Start server
        server.start();
    }
}

