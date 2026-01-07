package com.jinguan.rpc.api;

/**
 * Hello 服务接口 - 用于RPC功能演示
 * Hello service interface - used for RPC demonstration
 * 
 * 这是一个简单的服务接口，用于演示基础的RPC功能
 * 在实际项目中，可以定义任何业务接口，只要客户端和服务端共享该接口定义即可
 * This is a simple service that demonstrates basic RPC functionality
 *
 * @author JinGuan
 * @version 1.0.0
 */
public interface HelloService {
    
    /**
     * 向指定的人问好
     * Say hello to someone
     * 
     * 这是一个示例方法，接收一个名字参数，返回问候语
     * 实际的RPC调用流程：
     * 1. 客户端调用该方法
     * 2. RPC框架将方法调用信息打包成RpcRequest
     * 3. 通过网络发送到服务端
     * 4. 服务端接收请求，通过反射调用实现类的该方法
     * 5. 将返回结果打包成RpcResponse发回客户端
     * 6. 客户端收到响应并返回结果
     *
     * @param name 要问候的人的名字
     * @return 问候消息字符串
     */
    String hello(String name);
    
}

