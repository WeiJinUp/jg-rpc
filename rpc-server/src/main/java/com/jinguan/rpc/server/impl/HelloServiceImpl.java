package com.jinguan.rpc.server.impl;

import com.jinguan.rpc.api.HelloService;

/**
 * Hello service implementation
 * This is a simple implementation for demonstration purposes
 *
 * @author JinGuan
 * @version 1.0.0
 */
public class HelloServiceImpl implements HelloService {
    
    @Override
    public String hello(String name) {
        String message = "Hello, " + name + "! Welcome to JG-RPC Framework.";
        System.out.println("[HelloServiceImpl] Processing request: hello(" + name + ")");
        return message;
    }
}

