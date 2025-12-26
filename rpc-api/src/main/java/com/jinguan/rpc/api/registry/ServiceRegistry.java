package com.jinguan.rpc.api.registry;

import java.net.InetSocketAddress;

/**
 * Service Registry Interface
 * 
 * Provides service registration functionality
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public interface ServiceRegistry {
    
    /**
     * Register a service with the given address
     * 
     * @param serviceName service name (usually interface name)
     * @param address service provider address
     */
    void register(String serviceName, InetSocketAddress address);
    
    /**
     * Unregister a service
     * 
     * @param serviceName service name
     * @param address service provider address
     */
    void unregister(String serviceName, InetSocketAddress address);
    
    /**
     * Unregister all services at the given address
     * 
     * @param address service provider address
     */
    void unregisterAll(InetSocketAddress address);
}

