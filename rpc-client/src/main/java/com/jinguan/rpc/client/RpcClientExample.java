package com.jinguan.rpc.client;

import com.jinguan.rpc.api.HelloService;
import com.jinguan.rpc.api.dto.RpcRequest;
import com.jinguan.rpc.api.dto.RpcResponse;

/**
 * RPC Client Example - demonstrates various use cases
 *
 * @author JinGuan
 * @version 1.0.0
 */
public class RpcClientExample {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   JG-RPC Client Examples              ");
        System.out.println("========================================\n");
        
        RpcClient rpcClient = new RpcClient("localhost", 8888);
        
        // Example 1: Single RPC call
        System.out.println("Example 1: Single RPC Call");
        System.out.println("----------------------------------------");
        singleCall(rpcClient, "Alice");
        
        System.out.println("\n");
        
        // Example 2: Multiple RPC calls
        System.out.println("Example 2: Multiple RPC Calls");
        System.out.println("----------------------------------------");
        String[] names = {"Bob", "Charlie", "David", "Eve"};
        for (String name : names) {
            singleCall(rpcClient, name);
        }
        
        System.out.println("\n========================================");
        System.out.println("   All Examples Completed!             ");
        System.out.println("========================================");
    }
    
    /**
     * Execute a single RPC call
     */
    private static void singleCall(RpcClient rpcClient, String name) {
        try {
            // Create RPC request
            RpcRequest request = new RpcRequest(
                    HelloService.class.getName(),
                    "hello",
                    new Object[]{name},
                    new Class<?>[]{String.class}
            );
            
            // Send request
            RpcResponse response = rpcClient.sendRequest(request);
            
            // Handle response
            if (response.isSuccess()) {
                System.out.println("✓ Success: " + response.getData());
            } else {
                System.out.println("✗ Failed: " + response.getErrorMessage());
            }
            
        } catch (Exception e) {
            System.out.println("✗ Exception: " + e.getMessage());
        }
    }
}

