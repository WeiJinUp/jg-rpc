package com.jinguan.rpc.server.netty;

import com.jinguan.rpc.api.codec.RpcDecoder;
import com.jinguan.rpc.api.codec.RpcEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Netty-based RPC Server
 * 
 * High-performance network server using Netty's event-driven model
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class NettyRpcServer {
    
    private static final Logger logger = LoggerFactory.getLogger(NettyRpcServer.class);
    
    private static final int DEFAULT_PORT = 9000;
    
    /**
     * Service registry - maps interface name to service implementation
     */
    private final Map<String, Object> serviceMap = new HashMap<>();
    
    /**
     * Server port
     */
    private final int port;
    
    /**
     * Boss event loop group - handles client connections
     */
    private EventLoopGroup bossGroup;
    
    /**
     * Worker event loop group - handles I/O operations
     */
    private EventLoopGroup workerGroup;
    
    /**
     * Server channel
     */
    private Channel serverChannel;
    
    public NettyRpcServer() {
        this(DEFAULT_PORT);
    }
    
    public NettyRpcServer(int port) {
        this.port = port;
    }
    
    /**
     * Register a service implementation
     * 
     * @param service service implementation instance
     */
    public void register(Object service) {
        if (service == null) {
            throw new IllegalArgumentException("Service cannot be null");
        }
        
        Class<?>[] interfaces = service.getClass().getInterfaces();
        if (interfaces.length == 0) {
            throw new IllegalArgumentException("Service must implement at least one interface");
        }
        
        // Register all interfaces
        for (Class<?> intf : interfaces) {
            String interfaceName = intf.getName();
            serviceMap.put(interfaceName, service);
            logger.info("Registered service: {}", interfaceName);
        }
    }
    
    /**
     * Get service instance by interface name
     * 
     * @param interfaceName interface name
     * @return service instance
     */
    public Object getService(String interfaceName) {
        return serviceMap.get(interfaceName);
    }
    
    /**
     * Start the Netty RPC server
     */
    public void start() {
        // Create event loop groups
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // Idle state handler - detect inactive connections
                            pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            
                            // Codec
                            pipeline.addLast(new RpcDecoder());
                            pipeline.addLast(new RpcEncoder());
                            
                            // Business logic handler
                            pipeline.addLast(new NettyRpcServerHandler(NettyRpcServer.this));
                        }
                    });
            
            // Bind and start to accept incoming connections
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            
            logger.info("========================================");
            logger.info("  Netty RPC Server started");
            logger.info("  Port: {}", port);
            logger.info("  Registered services: {}", serviceMap.size());
            logger.info("========================================");
            
            // Wait until the server socket is closed
            serverChannel.closeFuture().sync();
            
        } catch (InterruptedException e) {
            logger.error("Server interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            shutdown();
        }
    }
    
    /**
     * Shutdown the server gracefully
     */
    public void shutdown() {
        logger.info("Shutting down Netty RPC server...");
        
        if (serverChannel != null) {
            serverChannel.close();
        }
        
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        
        logger.info("Netty RPC server stopped");
    }
}

