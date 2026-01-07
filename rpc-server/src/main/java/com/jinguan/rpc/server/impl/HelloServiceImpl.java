package com.jinguan.rpc.server.impl;

import com.jinguan.rpc.api.HelloService;

/**
 * Hello 服务实现类
 * Hello service implementation
 * 
 * 这是HelloService接口的具体实现，运行在服务端
 * 该类的实例会被注册到RpcServer中，当客户端发起调用时，
 * 服务端通过反射机制调用该类中的方法并返回结果
 * This is a simple implementation for demonstration purposes
 *
 * @author JinGuan
 * @version 1.0.0
 */
public class HelloServiceImpl implements HelloService {
    
    /**
     * 实现hello方法 - 生成问候语
     * Implementation of hello method - generates greeting message
     * 
     * 方法执行流程：
     * 1. 接收客户端传递过来的name参数
     * 2. 拼接生成问候消息字符串
     * 3. 在服务端控制台打印处理日志
     * 4. 返回问候消息给客户端
     * 
     * @param name 要问候的人的名字
     * @return 包含问候信息的字符串
     */
    @Override
    public String hello(String name) {
        // 拼接问候消息
        String message = "Hello, " + name + "! Welcome to JG-RPC Framework.";
        // 记录服务端处理日志，方便调试和监控
        System.out.println("[HelloServiceImpl] Processing request: hello(" + name + ")");
        // 返回结果
        return message;
    }
}

