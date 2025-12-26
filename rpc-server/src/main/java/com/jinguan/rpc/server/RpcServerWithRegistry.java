package com.jinguan.rpc.server;

import com.jinguan.rpc.api.registry.ServiceRegistry;
import com.jinguan.rpc.server.netty.NettyRpcServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * RPC Server with Service Registry
 * 
 * Enhanced server with automatic service registration to Zookeeper
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class RpcServerWithRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcServerWithRegistry.class);
    
    private final NettyRpcServer nettyServer;
    private final ServiceRegistry serviceRegistry;
    private final InetSocketAddress serverAddress;
    
    /**
     * List of registered services for cleanup
     */
    private final List<String> registeredServices = new ArrayList<>();
    
    public RpcServerWithRegistry(int port, ServiceRegistry serviceRegistry) {
        this.nettyServer = new NettyRpcServer(port);
        this.serviceRegistry = serviceRegistry;
        
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            this.serverAddress = new InetSocketAddress(host, port);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Failed to get local host address", e);
        }
        
        // Add shutdown hook for graceful shutdown
        addShutdownHook();
    }
    
    /**
     * Register and publish a service
     * 
     * @param service service implementation instance
     */
    public void publishService(Object service) {
        // Register service locally
        nettyServer.register(service);
        
        // Register service to Zookeeper
        Class<?>[] interfaces = service.getClass().getInterfaces();
        for (Class<?> intf : interfaces) {
            String serviceName = intf.getName();
            serviceRegistry.register(serviceName, serverAddress);
            registeredServices.add(serviceName);
            logger.info("Service published to registry: {} -> {}", serviceName, serverAddress);
        }
    }
    
    /**
     * Start the server
     */
    public void start() {
        logger.info("Starting RPC server with service registry...");
        nettyServer.start();
    }
    
    /**
     * Graceful shutdown
     */
    public void shutdown() {
        logger.info("========================================");
        logger.info("  Starting graceful shutdown...");
        logger.info("========================================");
        
        // Step 1: Unregister all services from Zookeeper
        logger.info("Step 1: Unregistering services from Zookeeper...");
        try {
            serviceRegistry.unregisterAll(serverAddress);
            logger.info("✓ All services unregistered");
        } catch (Exception e) {
            logger.error("Error unregistering services", e);
        }
        
        // Step 2: Wait for ongoing requests to complete
        logger.info("Step 2: Waiting for ongoing requests to complete...");
        try {
            Thread.sleep(5000); // Wait 5 seconds
            logger.info("✓ Wait completed");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Step 3: Shutdown Netty server
        logger.info("Step 3: Shutting down Netty server...");
        nettyServer.shutdown();
        logger.info("✓ Netty server stopped");
        
        logger.info("========================================");
        logger.info("  Graceful shutdown completed");
        logger.info("========================================");
    }
    
    /**
     * Add JVM shutdown hook
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown hook triggered");
            shutdown();
        }, "shutdown-hook"));
    }
}

