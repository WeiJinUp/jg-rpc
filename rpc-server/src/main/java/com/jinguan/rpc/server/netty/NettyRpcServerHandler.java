package com.jinguan.rpc.server.netty;

import com.jinguan.rpc.api.dto.RpcRequest;
import com.jinguan.rpc.api.dto.RpcResponse;
import com.jinguan.rpc.api.protocol.RpcMessage;
import com.jinguan.rpc.api.protocol.RpcProtocol;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Netty RPC Server Handler
 * 
 * Handles RPC requests and invokes service methods
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class NettyRpcServerHandler extends SimpleChannelInboundHandler<RpcMessage> {
    
    private static final Logger logger = LoggerFactory.getLogger(NettyRpcServerHandler.class);
    
    private final NettyRpcServer server;
    
    public NettyRpcServerHandler(NettyRpcServer server) {
        this.server = server;
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {
        logger.debug("Received message: {}", msg);
        
        byte messageType = msg.getMessageType();
        
        // Handle heartbeat
        if (messageType == RpcProtocol.MessageType.HEARTBEAT_REQUEST) {
            handleHeartbeat(ctx, msg);
            return;
        }
        
        // Handle RPC request
        if (messageType == RpcProtocol.MessageType.REQUEST) {
            handleRpcRequest(ctx, msg);
        }
    }
    
    /**
     * Handle heartbeat request
     */
    private void handleHeartbeat(ChannelHandlerContext ctx, RpcMessage request) {
        logger.debug("Received heartbeat from {}", ctx.channel().remoteAddress());
        
        RpcMessage response = new RpcMessage();
        response.setMessageType(RpcProtocol.MessageType.HEARTBEAT_RESPONSE);
        response.setSerializerType(request.getSerializerType());
        response.setData("pong");
        
        ctx.writeAndFlush(response);
    }
    
    /**
     * Handle RPC request
     */
    private void handleRpcRequest(ChannelHandlerContext ctx, RpcMessage message) {
        RpcRequest request = (RpcRequest) message.getData();
        logger.info("Processing RPC request: interface={}, method={}", 
                request.getInterfaceName(), request.getMethodName());
        
        // Process request and get response
        RpcResponse response = processRequest(request);
        response.setRequestId(request.getRequestId());
        
        // Create response message
        RpcMessage responseMessage = new RpcMessage();
        responseMessage.setMessageType(RpcProtocol.MessageType.RESPONSE);
        responseMessage.setSerializerType(message.getSerializerType());
        responseMessage.setRequestId(request.getRequestId());
        responseMessage.setData(response);
        
        // Send response
        ctx.writeAndFlush(responseMessage).addListener(future -> {
            if (future.isSuccess()) {
                logger.debug("Response sent successfully");
            } else {
                logger.error("Failed to send response", future.cause());
            }
        });
    }
    
    /**
     * Process RPC request using reflection
     */
    private RpcResponse processRequest(RpcRequest request) {
        try {
            // Get service instance
            String interfaceName = request.getInterfaceName();
            Object serviceImpl = server.getService(interfaceName);
            
            if (serviceImpl == null) {
                return RpcResponse.fail("Service not found: " + interfaceName);
            }
            
            // Get method
            String methodName = request.getMethodName();
            Class<?>[] paramTypes = request.getParamTypes();
            Method method = serviceImpl.getClass().getMethod(methodName, paramTypes);
            
            // Invoke method
            Object[] parameters = request.getParameters();
            Object result = method.invoke(serviceImpl, parameters);
            
            // If method returns CompletableFuture, wait for it to complete
            if (result instanceof java.util.concurrent.CompletableFuture) {
                result = ((java.util.concurrent.CompletableFuture<?>) result).get();
            }
            
            logger.info("Method invoked successfully: {}.{}", interfaceName, methodName);
            return RpcResponse.success(result);
            
        } catch (Exception e) {
            logger.error("Failed to process request", e);
            String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            return RpcResponse.fail("Server error: " + errorMessage);
        }
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                logger.warn("Client {} idle timeout, closing connection", ctx.channel().remoteAddress());
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception caught in server handler", cause);
        ctx.close();
    }
}

