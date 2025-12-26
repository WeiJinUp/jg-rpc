package com.jinguan.rpc.api.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * Load Balancer Interface
 * 
 * Selects a service instance from multiple providers
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public interface LoadBalancer {
    
    /**
     * Select one address from the given list
     * 
     * @param addresses available service addresses
     * @param serviceName service name (for consistent hashing, etc.)
     * @return selected address
     */
    InetSocketAddress select(List<InetSocketAddress> addresses, String serviceName);
}

