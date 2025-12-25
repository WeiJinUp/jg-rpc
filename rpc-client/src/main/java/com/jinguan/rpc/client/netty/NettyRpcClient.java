package com.jinguan.rpc.client.netty;

import com.jinguan.rpc.api.codec.RpcDecoder;
import com.jinguan.rpc.api.codec.RpcEncoder;
import com.jinguan.rpc.api.dto.RpcRequest;
import com.jinguan.rpc.api.dto.RpcResponse;
import com.jinguan.rpc.api.protocol.RpcMessage;
import com.jinguan.rpc.api.protocol.RpcProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Netty-based RPC Client
 * 
 * High-performance client using Netty with connection reuse
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class NettyRpcClient {
    
    private static final Logger logger = LoggerFactory.getLogger(NettyRpcClient.class);
    
    /**
     * Pending requests - mapping requestId to CompletableFuture
     */
    private static final Map<String, CompletableFuture<RpcResponse>> PENDING_REQUESTS = new ConcurrentHashMap<>();
    
    private final String host;
    private final int port;
    private final byte serializerType;
    
    private EventLoopGroup eventLoopGroup;
    private Channel channel;
    private Bootstrap bootstrap;
    
    public NettyRpcClient(String host, int port) {
        this(host, port, RpcProtocol.SerializerType.JSON);
    }
    
    public NettyRpcClient(String host, int port, byte serializerType) {
        this.host = host;
        this.port = port;
        this.serializerType = serializerType;
    }
    
    /**
     * Connect to server
     */
    public void connect() {
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        
                        // Codec
                        pipeline.addLast(new RpcDecoder());
                        pipeline.addLast(new RpcEncoder());
                        
                        // Client handler
                        pipeline.addLast(new NettyRpcClientHandler());
                    }
                });
        
        try {
            ChannelFuture future = bootstrap.connect(host, port).sync();
            channel = future.channel();
            logger.info("Connected to server: {}:{}", host, port);
        } catch (InterruptedException e) {
            logger.error("Failed to connect to server", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Connection failed", e);
        }
    }
    
    /**
     * Send RPC request and wait for response
     * 
     * @param request RPC request
     * @return RPC response
     */
    public RpcResponse sendRequest(RpcRequest request) {
        if (channel == null || !channel.isActive()) {
            connect();
        }
        
        try {
            // Generate request ID
            String requestId = generateRequestId();
            request.setRequestId(requestId);
            
            // Create RpcMessage
            RpcMessage message = new RpcMessage();
            message.setSerializerType(serializerType);
            message.setMessageType(RpcProtocol.MessageType.REQUEST);
            message.setRequestId(requestId);
            message.setData(request);
            
            // Create CompletableFuture for this request
            CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();
            PENDING_REQUESTS.put(requestId, responseFuture);
            
            // Send request
            channel.writeAndFlush(message).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    logger.debug("Request sent successfully: {}", requestId);
                } else {
                    logger.error("Failed to send request: {}", requestId, future.cause());
                    responseFuture.completeExceptionally(future.cause());
                    PENDING_REQUESTS.remove(requestId);
                }
            });
            
            // Wait for response (with timeout)
            return responseFuture.get(10, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            logger.error("Failed to send request", e);
            throw new RuntimeException("RPC call failed", e);
        }
    }
    
    /**
     * Complete pending request
     */
    public static void completePendingRequest(String requestId, RpcResponse response) {
        CompletableFuture<RpcResponse> future = PENDING_REQUESTS.remove(requestId);
        if (future != null) {
            future.complete(response);
        } else {
            logger.warn("No pending request found for requestId: {}", requestId);
        }
    }
    
    /**
     * Close client and release resources
     */
    public void close() {
        if (channel != null) {
            channel.close();
        }
        
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }
        
        logger.info("Client closed");
    }
    
    /**
     * Generate unique request ID
     */
    private String generateRequestId() {
        return Thread.currentThread().getId() + "-" + System.nanoTime();
    }
    
    /**
     * Client handler for processing responses
     */
    private static class NettyRpcClientHandler extends SimpleChannelInboundHandler<RpcMessage> {
        
        private static final Logger logger = LoggerFactory.getLogger(NettyRpcClientHandler.class);
        
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {
            logger.debug("Received response: {}", msg);
            
            if (msg.getMessageType() == RpcProtocol.MessageType.RESPONSE) {
                RpcResponse response = (RpcResponse) msg.getData();
                // Get requestId from response data, not from message
                String requestId = response.getRequestId();
                
                // Complete the pending request
                NettyRpcClient.completePendingRequest(requestId, response);
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Exception in client handler", cause);
            ctx.close();
        }
    }
}

