package com.jinguan.rpc.client.netty;

import com.jinguan.rpc.api.HelloService;
import com.jinguan.rpc.client.proxy.RpcClientProxy;

/**
 * Netty客户端启动类 - 第二阶段
 * Netty Client Bootstrap - Phase 2
 * 
 * 演示Netty客户端和动态代理的使用
 * 
 * 第二阶段特性：
 * 1. 基于Netty的高性能客户端
 * 2. JDK动态代理实现透明RPC调用
 * 3. 连接复用和异步通信
 * 
 * 使用动态代理的优势：
 * - 客户端代码更简洁
 * - 像调用本地方法一样调用远程服务
 * - 类型安全，编译期检查
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class NettyClientBootstrap {
    
    /**
     * 主方法 - 演示动态代理的RPC调用
     * 
     * 演示步骤：
     * 1. 创建Netty RPC客户端
     * 2. 创建代理工厂
     * 3. 获取服务接口的代理对象
     * 4. 像调用本地方法一样调用远程方法
     * 5. 关闭客户端
     * 
     * @param args 命令行参数
     */
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

