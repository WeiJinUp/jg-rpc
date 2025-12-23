package com.jinguan.rpc.api;

/**
 * Hello service interface - used for RPC demonstration
 * This is a simple service that demonstrates basic RPC functionality
 *
 * @author JinGuan
 * @version 1.0.0
 */
public interface HelloService {
    
    /**
     * Say hello to someone
     *
     * @param name the name to greet
     * @return greeting message
     */
    String hello(String name);
    
}

