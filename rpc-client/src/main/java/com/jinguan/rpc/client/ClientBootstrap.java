package com.jinguan.rpc.client;

import com.jinguan.rpc.api.HelloService;
import com.jinguan.rpc.api.dto.RpcRequest;
import com.jinguan.rpc.api.dto.RpcResponse;

/**
 * 客户端启动类 - RPC客户端的主入口，用于测试RPC调用
 * Client Bootstrap - main entry point for testing RPC client
 * 
 * 该类演示了如何使用RPC框架进行远程方法调用
 *
 * @author JinGuan
 * @version 1.0.0
 */
public class ClientBootstrap {
    
    /**
     * 主方法 - 演示RPC客户端的使用
     * Main method - demonstrates RPC client usage
     * 
     * 执行流程：
     * 1. 创建RPC客户端实例，指定服务器地址和端口
     * 2. 准备RPC请求对象，指定要调用的接口、方法、参数
     * 3. 发送请求到服务器并等待响应
     * 4. 处理响应结果，判断调用是否成功
     * 5. 如果发生异常，捕获并打印错误信息
     * 
     * @param args 命令行参数（本示例未使用）
     */
    public static void main(String[] args) {
        // 打印启动横幅
        System.out.println("========================================");
        System.out.println("   JG-RPC Client Starting...           ");
        System.out.println("========================================");
        
        // 步骤1：创建RPC客户端实例
        // localhost表示连接本地服务器，8888是服务器监听端口
        RpcClient rpcClient = new RpcClient("localhost", 8888);
        
        try {
            // 步骤2：准备RPC请求对象
            // 需要指定4个关键信息：接口名、方法名、参数值、参数类型
            
            // 2.1 获取接口的全限定名
            String interfaceName = HelloService.class.getName();
            
            // 2.2 指定要调用的方法名
            String methodName = "hello";
            
            // 2.3 准备方法参数值
            Object[] parameters = new Object[]{"JinGuan"};
            
            // 2.4 准备方法参数类型
            // 参数类型用于在服务端准确定位方法（处理方法重载）
            Class<?>[] paramTypes = new Class<?>[]{String.class};
            
            // 创建RPC请求对象
            RpcRequest request = new RpcRequest(interfaceName, methodName, parameters, paramTypes);
            
            // 步骤3：发送请求并获取响应
            // 这是一个同步阻塞调用，会等待服务器返回结果
            RpcResponse response = rpcClient.sendRequest(request);
            
            // 步骤4：处理响应结果
            if (response.isSuccess()) {
                // 调用成功，打印返回结果
                System.out.println("\n========================================");
                System.out.println("   RPC Call Successful!                ");
                System.out.println("========================================");
                System.out.println("Result: " + response.getData());
            } else {
                // 调用失败，打印错误信息
                System.out.println("\n========================================");
                System.out.println("   RPC Call Failed!                    ");
                System.out.println("========================================");
                System.out.println("Error: " + response.getErrorMessage());
            }
            
        } catch (Exception e) {
            // 步骤5：捕获异常（如网络异常、超时等）
            System.err.println("\n========================================");
            System.err.println("   Exception Occurred!                  ");
            System.err.println("========================================");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

