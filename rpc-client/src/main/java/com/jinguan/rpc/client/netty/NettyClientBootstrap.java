package com.jinguan.rpc.client.netty;

import com.jinguan.rpc.api.HelloService;
import com.jinguan.rpc.client.proxy.RpcClientProxy;

/**
 * Netty Client Bootstrap - Phase 2
 * 
 * Demonstrates dynamic proxy usage for transparent RPC calls
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class NettyClientBootstrap {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   JG-RPC Netty Client Starting...     ");
        System.out.println("   Phase 2: Dynamic Proxy Demo         ");
        System.out.println("========================================");
        System.out.println();
        
        // Create Netty client
        NettyRpcClient client = new NettyRpcClient("localhost", 9000);
        
        try {
            // Create proxy factory
            RpcClientProxy proxyFactory = new RpcClientProxy(client);
            
            // Get proxy instance - looks like local object!
            HelloService helloService = proxyFactory.getProxy(HelloService.class);
            
            // Call remote method just like local method call
            System.out.println("Calling remote method...");
            String result = helloService.hello("Netty Dynamic Proxy");
            
            System.out.println();
            System.out.println("========================================");
            System.out.println("   RPC Call Successful!                ");
            System.out.println("========================================");
            System.out.println("Result: " + result);
            System.out.println();
            
            // Multiple calls
            System.out.println("Making multiple calls...");
            for (int i = 1; i <= 3; i++) {
                String response = helloService.hello("User-" + i);
                System.out.println(i + ". " + response);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close client
            client.close();
        }
        
        System.out.println();
        System.out.println("========================================");
        System.out.println("   Demo Completed!                     ");
        System.out.println("========================================");
    }
}

