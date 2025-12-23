package com.jinguan.rpc.client;

import com.jinguan.rpc.api.HelloService;
import com.jinguan.rpc.api.dto.RpcRequest;
import com.jinguan.rpc.api.dto.RpcResponse;

/**
 * Client Bootstrap - main entry point for testing RPC client
 *
 * @author JinGuan
 * @version 1.0.0
 */
public class ClientBootstrap {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   JG-RPC Client Starting...           ");
        System.out.println("========================================");
        
        // Create RPC client
        RpcClient rpcClient = new RpcClient("localhost", 8888);
        
        try {
            // Prepare RPC request
            String interfaceName = HelloService.class.getName();
            String methodName = "hello";
            Object[] parameters = new Object[]{"JinGuan"};
            Class<?>[] paramTypes = new Class<?>[]{String.class};
            
            RpcRequest request = new RpcRequest(interfaceName, methodName, parameters, paramTypes);
            
            // Send request and get response
            RpcResponse response = rpcClient.sendRequest(request);
            
            // Process response
            if (response.isSuccess()) {
                System.out.println("\n========================================");
                System.out.println("   RPC Call Successful!                ");
                System.out.println("========================================");
                System.out.println("Result: " + response.getData());
            } else {
                System.out.println("\n========================================");
                System.out.println("   RPC Call Failed!                    ");
                System.out.println("========================================");
                System.out.println("Error: " + response.getErrorMessage());
            }
            
        } catch (Exception e) {
            System.err.println("\n========================================");
            System.err.println("   Exception Occurred!                  ");
            System.err.println("========================================");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

