package com.jinguan.rpc.server.registry;

import com.jinguan.rpc.api.registry.ServiceRegistry;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Zookeeper-based Service Registry
 * 
 * Registers services as ephemeral nodes in Zookeeper
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class ZookeeperServiceRegistry implements ServiceRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperServiceRegistry.class);
    
    /**
     * Zookeeper root path for RPC services
     */
    private static final String ZK_ROOT_PATH = "/jg-rpc";
    
    /**
     * Curator client
     */
    private final CuratorFramework zkClient;
    
    /**
     * Registered service paths (for cleanup)
     */
    private final List<String> registeredPaths = new ArrayList<>();
    
    public ZookeeperServiceRegistry(String zkAddress) {
        // Create Curator client with retry policy
        this.zkClient = CuratorFrameworkFactory.builder()
                .connectString(zkAddress)
                .sessionTimeoutMs(5000)
                .connectionTimeoutMs(3000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        
        // Start client
        zkClient.start();
        logger.info("Zookeeper client started: {}", zkAddress);
    }
    
    @Override
    public void register(String serviceName, InetSocketAddress address) {
        try {
            // Create service path: /jg-rpc/{serviceName}/providers/{ip:port}
            String servicePath = ZK_ROOT_PATH + "/" + serviceName;
            String addressStr = address.getHostString() + ":" + address.getPort();
            String providerPath = servicePath + "/providers/" + addressStr;
            
            // Create parent path if not exists
            if (zkClient.checkExists().forPath(servicePath) == null) {
                zkClient.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(servicePath);
            }
            
            if (zkClient.checkExists().forPath(servicePath + "/providers") == null) {
                zkClient.create()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(servicePath + "/providers");
            }
            
            // Create ephemeral node for this provider
            // Ephemeral node will be automatically deleted when connection is lost
            if (zkClient.checkExists().forPath(providerPath) == null) {
                zkClient.create()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath(providerPath);
                
                registeredPaths.add(providerPath);
                logger.info("Service registered: {} -> {}", serviceName, addressStr);
            }
            
        } catch (Exception e) {
            logger.error("Failed to register service: {}", serviceName, e);
            throw new RuntimeException("Service registration failed", e);
        }
    }
    
    @Override
    public void unregister(String serviceName, InetSocketAddress address) {
        try {
            String addressStr = address.getHostString() + ":" + address.getPort();
            String providerPath = ZK_ROOT_PATH + "/" + serviceName + "/providers/" + addressStr;
            
            if (zkClient.checkExists().forPath(providerPath) != null) {
                zkClient.delete().forPath(providerPath);
                registeredPaths.remove(providerPath);
                logger.info("Service unregistered: {} -> {}", serviceName, addressStr);
            }
            
        } catch (Exception e) {
            logger.error("Failed to unregister service: {}", serviceName, e);
        }
    }
    
    @Override
    public void unregisterAll(InetSocketAddress address) {
        logger.info("Unregistering all services...");
        
        // Copy to avoid ConcurrentModificationException
        List<String> pathsToRemove = new ArrayList<>(registeredPaths);
        
        for (String path : pathsToRemove) {
            try {
                if (zkClient.checkExists().forPath(path) != null) {
                    zkClient.delete().forPath(path);
                }
            } catch (Exception e) {
                logger.error("Failed to delete path: {}", path, e);
            }
        }
        
        registeredPaths.clear();
        logger.info("All services unregistered");
    }
    
    /**
     * Close Zookeeper client
     */
    public void close() {
        if (zkClient != null) {
            zkClient.close();
            logger.info("Zookeeper client closed");
        }
    }
}

