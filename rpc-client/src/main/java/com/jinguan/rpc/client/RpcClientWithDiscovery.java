package com.jinguan.rpc.client;

import com.jinguan.rpc.api.loadbalance.LoadBalancer;
import com.jinguan.rpc.api.registry.ServiceDiscovery;
import com.jinguan.rpc.client.loadbalance.RoundRobinLoadBalancer;
import com.jinguan.rpc.client.netty.NettyRpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RPC Client with Service Discovery and Load Balancing
 * 
 * Enhanced client that supports automatic service discovery and load balancing
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class RpcClientWithDiscovery {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcClientWithDiscovery.class);
    
    private final ServiceDiscovery serviceDiscovery;
    private final LoadBalancer loadBalancer;
    
    /**
     * Cache of NettyRpcClient instances by address
     */
    private final Map<String, NettyRpcClient> clientCache = new HashMap<>();
    
    public RpcClientWithDiscovery(ServiceDiscovery serviceDiscovery) {
        this(serviceDiscovery, new RoundRobinLoadBalancer());
    }
    
    public RpcClientWithDiscovery(ServiceDiscovery serviceDiscovery, LoadBalancer loadBalancer) {
        this.serviceDiscovery = serviceDiscovery;
        this.loadBalancer = loadBalancer;
        logger.info("RPC client initialized with discovery and load balancing");
    }
    
    /**
     * Get RPC client for the given service
     * 
     * Automatically discovers service and selects instance using load balancer
     * 
     * @param serviceName service name
     * @return NettyRpcClient instance
     */
    public NettyRpcClient getClient(String serviceName) {
        // Discover all available instances
        List<InetSocketAddress> addresses = serviceDiscovery.discoverAll(serviceName);
        
        if (addresses == null || addresses.isEmpty()) {
            throw new RuntimeException("No available service found: " + serviceName);
        }
        
        // Select one using load balancer
        InetSocketAddress selected = loadBalancer.select(addresses, serviceName);
        logger.debug("Selected server: {} for service: {}", selected, serviceName);
        
        // Get or create client
        String key = selected.getHostString() + ":" + selected.getPort();
        return clientCache.computeIfAbsent(key, k -> {
            logger.info("Creating new client for: {}", selected);
            return new NettyRpcClient(selected.getHostString(), selected.getPort());
        });
    }
    
    /**
     * Close all clients and release resources
     */
    public void close() {
        logger.info("Closing all RPC clients...");
        
        for (NettyRpcClient client : clientCache.values()) {
            try {
                client.close();
            } catch (Exception e) {
                logger.error("Error closing client", e);
            }
        }
        
        clientCache.clear();
        logger.info("All clients closed");
    }
}

