package com.jinguan.rpc.client.loadbalance;

import com.jinguan.rpc.api.loadbalance.LoadBalancer;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;

/**
 * Random Load Balancer
 * 
 * Randomly selects a service instance
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class RandomLoadBalancer implements LoadBalancer {
    
    private final Random random = new Random();
    
    @Override
    public InetSocketAddress select(List<InetSocketAddress> addresses, String serviceName) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        
        if (addresses.size() == 1) {
            return addresses.get(0);
        }
        
        // Randomly select one
        int index = random.nextInt(addresses.size());
        return addresses.get(index);
    }
}

