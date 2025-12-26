package com.jinguan.rpc.api.registry;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Service Discovery Interface
 * 
 * Provides service discovery functionality
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public interface ServiceDiscovery {
    
    /**
     * Discover a single service instance
     * 
     * @param serviceName service name
     * @return service address, or null if not found
     */
    InetSocketAddress discover(String serviceName);
    
    /**
     * Discover all instances of a service
     * 
     * @param serviceName service name
     * @return list of service addresses
     */
    List<InetSocketAddress> discoverAll(String serviceName);
}

