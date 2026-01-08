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
 * Netty RPC服务器处理器 - 处理RPC请求和心跳
 * Netty RPC Server Handler
 * 
 * 该Handler负责处理客户端发来的RPC请求和心跳消息
 * 
 * 核心功能：
 * 1. 接收并解析RPC请求消息
 * 2. 通过反射调用服务方法
 * 3. 封装响应并发送回客户端
 * 4. 处理心跳请求，保持连接活跃
 * 5. 检测空闲连接并关闭
 * 
 * 继承关系：
 * - SimpleChannelInboundHandler<RpcMessage>: 入站消息处理器
 * - 泛型参数指定只处理RpcMessage类型的消息
 * - 自动进行类型转换和消息释放
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class NettyRpcServerHandler extends SimpleChannelInboundHandler<RpcMessage> {
    
    private static final Logger logger = LoggerFactory.getLogger(NettyRpcServerHandler.class);
    
    /**
     * RPC服务器引用
     * 用于获取已注册的服务实例
     */
    private final NettyRpcServer server;
    
    /**
     * 构造函数
     * 
     * @param server RPC服务器实例
     */
    public NettyRpcServerHandler(NettyRpcServer server) {
        this.server = server;
    }
    
    /**
     * 处理接收到的消息 - 入口方法
     * Handle received message
     * 
     * 根据消息类型分发到不同的处理方法：
     * - HEARTBEAT_REQUEST: 心跳请求
     * - REQUEST: RPC请求
     * 
     * @param ctx Channel上下文
     * @param msg RPC消息对象
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {
        logger.debug("Received message: {}", msg);
        
        byte messageType = msg.getMessageType();
        
        // 处理心跳请求
        if (messageType == RpcProtocol.MessageType.HEARTBEAT_REQUEST) {
            handleHeartbeat(ctx, msg);
            return;
        }
        
        // 处理RPC请求
        if (messageType == RpcProtocol.MessageType.REQUEST) {
            handleRpcRequest(ctx, msg);
        }
    }
    
    /**
     * 处理心跳请求 - 保持连接活跃
     * Handle heartbeat request
     * 
     * 心跳机制用于：
     * 1. 检测连接是否存活
     * 2. 防止连接被防火墙断开
     * 3. 及时发现网络故障
     * 
     * @param ctx Channel上下文
     * @param request 心跳请求消息
     */
    private void handleHeartbeat(ChannelHandlerContext ctx, RpcMessage request) {
        logger.debug("Received heartbeat from {}", ctx.channel().remoteAddress());
        
        // 创建心跳响应
        RpcMessage response = new RpcMessage();
        response.setMessageType(RpcProtocol.MessageType.HEARTBEAT_RESPONSE);
        response.setSerializerType(request.getSerializerType());
        response.setData("pong");
        
        // 发送响应
        ctx.writeAndFlush(response);
    }
    
    /**
     * 处理RPC请求 - 核心业务逻辑
     * Handle RPC request
     * 
     * 处理流程：
     * 1. 提取RpcRequest对象
     * 2. 调用processRequest处理请求
     * 3. 封装响应为RpcMessage
     * 4. 发送响应给客户端
     * 
     * @param ctx Channel上下文
     * @param message RPC消息对象
     */
    private void handleRpcRequest(ChannelHandlerContext ctx, RpcMessage message) {
        RpcRequest request = (RpcRequest) message.getData();
        logger.info("Processing RPC request: interface={}, method={}", 
                request.getInterfaceName(), request.getMethodName());
        
        // 处理请求并获取响应
        RpcResponse response = processRequest(request);
        response.setRequestId(request.getRequestId());
        
        // 创建响应消息
        RpcMessage responseMessage = new RpcMessage();
        responseMessage.setMessageType(RpcProtocol.MessageType.RESPONSE);
        responseMessage.setSerializerType(message.getSerializerType());
        responseMessage.setRequestId(request.getRequestId());
        responseMessage.setData(response);
        
        // 发送响应
        ctx.writeAndFlush(responseMessage).addListener(future -> {
            if (future.isSuccess()) {
                logger.debug("Response sent successfully");
            } else {
                logger.error("Failed to send response", future.cause());
            }
        });
    }
    
    /**
     * 处理RPC请求 - 使用反射调用服务方法
     * Process RPC request using reflection
     * 
     * 与第一阶段类似，但增加了CompletableFuture支持
     * 
     * @param request RPC请求对象
     * @return RPC响应对象
     */
    private RpcResponse processRequest(RpcRequest request) {
        try {
            // 获取服务实例
            String interfaceName = request.getInterfaceName();
            Object serviceImpl = server.getService(interfaceName);
            
            if (serviceImpl == null) {
                return RpcResponse.fail("Service not found: " + interfaceName);
            }
            
            // 获取方法
            String methodName = request.getMethodName();
            Class<?>[] paramTypes = request.getParamTypes();
            Method method = serviceImpl.getClass().getMethod(methodName, paramTypes);
            
            // 调用方法
            Object[] parameters = request.getParameters();
            Object result = method.invoke(serviceImpl, parameters);
            
            // 如果方法返回CompletableFuture，等待完成
            // 这支持第三阶段的异步RPC调用
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
    
    /**
     * 用户事件触发 - 处理空闲连接
     * User event triggered
     * 
     * 当IdleStateHandler检测到连接空闲时触发该方法
     * 
     * @param ctx Channel上下文
     * @param evt 事件对象
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            // 如果30秒内没有读事件，关闭连接
            if (event.state() == IdleState.READER_IDLE) {
                logger.warn("Client {} idle timeout, closing connection", ctx.channel().remoteAddress());
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
    
    /**
     * 异常处理
     * Exception handling
     * 
     * @param ctx Channel上下文
     * @param cause 异常对象
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception caught in server handler", cause);
        ctx.close();
    }
}

