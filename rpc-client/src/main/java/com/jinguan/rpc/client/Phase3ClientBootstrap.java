package com.jinguan.rpc.client;

import com.jinguan.rpc.api.HelloService;
import com.jinguan.rpc.api.async.AsyncHelloService;
import com.jinguan.rpc.client.discovery.ZookeeperServiceDiscovery;
import com.jinguan.rpc.client.loadbalance.RoundRobinLoadBalancer;
import com.jinguan.rpc.client.proxy.RpcClientProxyWithDiscovery;

import java.util.concurrent.CompletableFuture;

/**
 * Phase 3 Client Bootstrap
 * 
 * Demonstrates service discovery, load balancing, and async calls
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class Phase3ClientBootstrap {
    
    private static final String ZK_ADDRESS = "localhost:2181";
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   JG-RPC Client Phase 3               ");
        System.out.println("   Production-Grade Features           ");
        System.out.println("========================================");
        System.out.println();
        
        // Create service discovery
        ZookeeperServiceDiscovery discovery = new ZookeeperServiceDiscovery(ZK_ADDRESS);
        
        // Create client manager with load balancer
        RpcClientWithDiscovery clientManager = new RpcClientWithDiscovery(
                discovery, 
                new RoundRobinLoadBalancer()
        );
        
        // Create proxy factory
        RpcClientProxyWithDiscovery proxyFactory = new RpcClientProxyWithDiscovery(clientManager);
        
        try {
            // Test 1: Synchronous call with service discovery
            System.out.println("Test 1: Synchronous Call with Service Discovery");
            System.out.println("------------------------------------------------");
            HelloService helloService = proxyFactory.getProxy(HelloService.class);
            String result = helloService.hello("Phase 3 User");
            System.out.println("Result: " + result);
            System.out.println();
            
            // Test 2: Multiple calls to test load balancing
            System.out.println("Test 2: Load Balancing (Round Robin)");
            System.out.println("------------------------------------------------");
            for (int i = 1; i <= 5; i++) {
                String res = helloService.hello("User-" + i);
                System.out.println(i + ". " + res);
            }
            System.out.println();
            
            // Test 3: Asynchronous call
            System.out.println("Test 3: Asynchronous Call");
            System.out.println("------------------------------------------------");
            AsyncHelloService asyncService = proxyFactory.getProxy(AsyncHelloService.class);
            
            System.out.println("Sending async request...");
            CompletableFuture<String> future = asyncService.helloAsync("Async User");
            System.out.println("Request sent (non-blocking)");
            
            // Do other work while waiting
            System.out.println("Doing other work...");
            Thread.sleep(100);
            
            // Get result when ready
            String asyncResult = future.get();
            System.out.println("Async result: " + asyncResult);
            System.out.println();
            
            // Test 4: Multiple async calls (parallel execution)
            System.out.println("Test 4: Parallel Async Calls");
            System.out.println("------------------------------------------------");
            System.out.println("Sending 3 async requests in parallel...");
            long start = System.currentTimeMillis();
            
            CompletableFuture<String> future1 = asyncService.helloAsync("Parallel-1");
            CompletableFuture<String> future2 = asyncService.helloAsync("Parallel-2");
            CompletableFuture<String> future3 = asyncService.helloAsync("Parallel-3");
            
            System.out.println("All requests sent (non-blocking)");
            System.out.println("Waiting for all results...");
            
            // Wait for all to complete
            CompletableFuture.allOf(future1, future2, future3).get();
            long elapsed = System.currentTimeMillis() - start;
            
            System.out.println("Results:");
            System.out.println("  1. " + future1.get());
            System.out.println("  2. " + future2.get());
            System.out.println("  3. " + future3.get());
            System.out.println("Time elapsed: " + elapsed + "ms (parallel execution)");
            System.out.println();
            
            // Test 5: Another async call (simplified)
            System.out.println("Test 5: Additional Async Call Test");
            System.out.println("------------------------------------------------");
            System.out.println("Sending another async request...");
            
            CompletableFuture<String> another = asyncService.helloAsync("Final User");
            System.out.println("Request sent (non-blocking)");
            
            String anotherResult = another.get();
            System.out.println("Result: " + anotherResult);
            System.out.println();
            
            System.out.println("========================================");
            System.out.println("   All Tests Completed Successfully!  ");
            System.out.println("========================================");
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup
            clientManager.close();
            discovery.close();
        }
    }
}

