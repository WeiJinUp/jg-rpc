package com.jinguan.rpc.client.discovery;

import com.jinguan.rpc.api.registry.ServiceDiscovery;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Zookeeper-based Service Discovery
 * 
 * Discovers services from Zookeeper
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class ZookeeperServiceDiscovery implements ServiceDiscovery {
    
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperServiceDiscovery.class);
    
    private static final String ZK_ROOT_PATH = "/jg-rpc";
    
    private final CuratorFramework zkClient;
    
    public ZookeeperServiceDiscovery(String zkAddress) {
        this.zkClient = CuratorFrameworkFactory.builder()
                .connectString(zkAddress)
                .sessionTimeoutMs(5000)
                .connectionTimeoutMs(3000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        
        zkClient.start();
        logger.info("Zookeeper client started for discovery: {}", zkAddress);
    }
    
    @Override
    public InetSocketAddress discover(String serviceName) {
        List<InetSocketAddress> addresses = discoverAll(serviceName);
        if (addresses == null || addresses.isEmpty()) {
            logger.warn("No available service found: {}", serviceName);
            return null;
        }
        
        // Return the first one (will be improved by load balancer)
        return addresses.get(0);
    }
    
    @Override
    public List<InetSocketAddress> discoverAll(String serviceName) {
        List<InetSocketAddress> addresses = new ArrayList<>();
        
        try {
            String providersPath = ZK_ROOT_PATH + "/" + serviceName + "/providers";
            
            // Check if service exists
            if (zkClient.checkExists().forPath(providersPath) == null) {
                logger.warn("Service not found in Zookeeper: {}", serviceName);
                return addresses;
            }
            
            // Get all provider nodes
            List<String> providerNodes = zkClient.getChildren().forPath(providersPath);
            
            for (String node : providerNodes) {
                // Parse address from node name (format: ip:port)
                String[] parts = node.split(":");
                if (parts.length == 2) {
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    addresses.add(new InetSocketAddress(host, port));
                }
            }
            
            logger.info("Discovered {} instances for service: {}", addresses.size(), serviceName);
            
        } catch (Exception e) {
            logger.error("Failed to discover service: {}", serviceName, e);
        }
        
        return addresses;
    }
    
    /**
     * Close Zookeeper client
     */
    public void close() {
        if (zkClient != null) {
            zkClient.close();
            logger.info("Zookeeper discovery client closed");
        }
    }
}

