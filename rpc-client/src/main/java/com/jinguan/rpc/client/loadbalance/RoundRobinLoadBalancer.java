package com.jinguan.rpc.client.loadbalance;

import com.jinguan.rpc.api.loadbalance.LoadBalancer;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round Robin Load Balancer
 * 
 * Distributes requests evenly across service instances
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class RoundRobinLoadBalancer implements LoadBalancer {
    
    /**
     * Counter for each service
     */
    private final ConcurrentHashMap<String, AtomicInteger> counterMap = new ConcurrentHashMap<>();
    
    @Override
    public InetSocketAddress select(List<InetSocketAddress> addresses, String serviceName) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        
        if (addresses.size() == 1) {
            return addresses.get(0);
        }
        
        // Get or create counter for this service
        AtomicInteger counter = counterMap.computeIfAbsent(serviceName, k -> new AtomicInteger(0));
        
        // Get next index (round robin)
        int index = counter.getAndIncrement() % addresses.size();
        
        return addresses.get(index);
    }
}

