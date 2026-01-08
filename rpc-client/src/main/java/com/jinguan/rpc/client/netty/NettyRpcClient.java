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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于Netty的RPC客户端 - 高性能异步客户端实现
 * Netty-based RPC Client
 * 
 * 该客户端使用Netty框架实现高性能的异步网络通信
 * 
 * 核心特性：
 * 1. 异步非阻塞I/O：基于Netty的事件驱动模型
 * 2. 连接复用：保持长连接，避免频繁建立连接的开销
 * 3. 异步调用：使用CompletableFuture实现异步RPC调用
 * 4. 请求响应匹配：通过requestId匹配请求和响应
 * 5. 自定义协议：支持自定义的二进制协议
 * 6. 可插拔序列化：支持多种序列化方式
 * 
 * 工作原理：
 * 1. 客户端发送请求时生成唯一的requestId
 * 2. 创建CompletableFuture并放入PENDING_REQUESTS
 * 3. 将请求发送到服务器
 * 4. 当收到响应时，根据requestId找到对应的Future并complete
 * 5. 客户端的sendRequest方法等待Future完成并返回结果
 * 
 * 相比第一阶段的Socket客户端的优势：
 * - 性能更高：Netty的零拷贝、内存池等优化
 * - 连接复用：避免每次调用都建立新连接
 * - 异步处理：支持并发发送多个请求
 * - 更好的编解码：使用自定义协议和编解码器
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class NettyRpcClient {
    
    private static final Logger logger = LoggerFactory.getLogger(NettyRpcClient.class);
    
    /**
     * 待处理请求Map - 存储请求ID到Future的映射
     * Pending requests - mapping requestId to CompletableFuture
     * 
     * 这是实现异步RPC的关键数据结构：
     * - Key: 请求的唯一标识requestId
     * - Value: 该请求对应的CompletableFuture，用于异步接收响应
     * 
     * 使用ConcurrentHashMap保证线程安全：
     * - 多个线程可能同时发送请求
     * - 响应可能在不同的线程中接收
     * 
     * 使用static的原因：
     * - 同一个客户端实例的所有请求共享一个Map
     * - 支持在不同的Handler中访问
     */
    private static final Map<String, CompletableFuture<RpcResponse>> PENDING_REQUESTS = new ConcurrentHashMap<>();
    
    /**
     * 服务器主机地址
     */
    private final String host;
    
    /**
     * 服务器端口号
     */
    private final int port;
    
    /**
     * 序列化类型
     * 该客户端使用的序列化方式（Java/JSON/Protobuf等）
     */
    private final byte serializerType;
    
    /**
     * Netty的事件循环组
     * 负责处理所有的I/O事件（连接、读、写等）
     */
    private EventLoopGroup eventLoopGroup;
    
    /**
     * Netty的Channel
     * 代表与服务器的连接，用于发送和接收数据
     */
    private Channel channel;
    
    /**
     * Netty的启动器
     * 用于配置和启动客户端
     */
    private Bootstrap bootstrap;
    
    /**
     * 构造函数 - 使用默认的JSON序列化
     * Constructor - uses default JSON serialization
     * 
     * @param host 服务器地址
     * @param port 服务器端口
     */
    public NettyRpcClient(String host, int port) {
        this(host, port, RpcProtocol.SerializerType.JSON);
    }
    
    /**
     * 构造函数 - 指定序列化类型
     * Constructor - specifies serialization type
     * 
     * @param host 服务器地址
     * @param port 服务器端口
     * @param serializerType 序列化类型
     */
    public NettyRpcClient(String host, int port, byte serializerType) {
        this.host = host;
        this.port = port;
        this.serializerType = serializerType;
    }
    
    /**
     * 连接到服务器 - 建立与服务器的Netty连接
     * Connect to server
     * 
     * 连接步骤（详细）：
     * 1. 创建EventLoopGroup（事件循环组）
     * 2. 创建Bootstrap（客户端启动器）
     * 3. 配置Bootstrap参数
     * 4. 配置Channel的Pipeline（处理链）
     * 5. 发起连接并等待完成
     * 6. 保存Channel引用供后续使用
     * 
     * EventLoopGroup作用：
     * - 管理多个EventLoop线程
     * - 每个EventLoop处理多个Channel的I/O事件
     * - 使用NIO实现高并发
     * 
     * ChannelPipeline作用：
     * - 责任链模式，处理入站和出站数据
     * - 解码器 -> 编码器 -> 业务Handler
     * - 数据流向：网络 -> Decoder -> Handler -> Encoder -> 网络
     */
    public void connect() {
        // 步骤1：创建事件循环组
        // NioEventLoopGroup是基于NIO的事件循环组，高性能
        eventLoopGroup = new NioEventLoopGroup();
        
        // 步骤2：创建客户端启动器
        bootstrap = new Bootstrap();
        
        // 步骤3：配置Bootstrap
        bootstrap.group(eventLoopGroup)  // 设置事件循环组
                .channel(NioSocketChannel.class)  // 使用NIO Socket Channel
                .option(ChannelOption.TCP_NODELAY, true)  // 禁用Nagle算法，减少延迟
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)  // 连接超时5秒
                .handler(new ChannelInitializer<SocketChannel>() {
                    /**
                     * 初始化Channel的Pipeline
                     * 该方法在Channel创建时被调用，用于设置处理器链
                     */
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // 步骤4：获取Pipeline并添加处理器
                        ChannelPipeline pipeline = ch.pipeline();
                        
                        // 添加解码器 - 处理入站数据（服务器 -> 客户端）
                        // 将字节流解码为RpcMessage对象
                        pipeline.addLast(new RpcDecoder());
                        
                        // 添加编码器 - 处理出站数据（客户端 -> 服务器）
                        // 将RpcMessage对象编码为字节流
                        pipeline.addLast(new RpcEncoder());
                        
                        // 添加客户端业务处理器
                        // 处理接收到的响应消息
                        pipeline.addLast(new NettyRpcClientHandler());
                    }
                });
        
        try {
            // 步骤5：发起连接
            // connect()返回ChannelFuture，代表异步操作
            // sync()等待连接完成
            ChannelFuture future = bootstrap.connect(host, port).sync();
            
            // 步骤6：获取并保存Channel
            channel = future.channel();
            logger.info("Connected to server: {}:{}", host, port);
            
        } catch (InterruptedException e) {
            // 连接被中断
            logger.error("Failed to connect to server", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Connection failed", e);
        }
    }
    
    /**
     * 发送RPC请求并等待响应 - 异步RPC调用的核心方法
     * Send RPC request and wait for response
     * 
     * 这是客户端的核心方法，实现了异步RPC调用的完整流程
     * 
     * 异步RPC实现原理（重要）：
     * 1. 为每个请求生成唯一的requestId
     * 2. 创建CompletableFuture并存入PENDING_REQUESTS
     * 3. 发送请求到服务器（异步发送）
     * 4. 当前线程调用future.get()等待响应（阻塞）
     * 5. Netty的I/O线程接收到响应后，调用completePendingRequest
     * 6. CompletableFuture被complete，等待的线程继续执行
     * 7. 返回响应结果
     * 
     * 为什么这样设计？
     * - 支持并发：多个线程可以同时发送请求
     * - 请求响应匹配：通过requestId正确匹配请求和响应
     * - 连接复用：多个请求共享同一个Channel
     * - 异步I/O：发送和接收在不同的线程中进行
     * 
     * @param request RPC请求对象
     * @return RPC响应对象
     * @throws RuntimeException 如果请求发送失败或超时
     */
    public RpcResponse sendRequest(RpcRequest request) {
        // 步骤1：检查连接状态，如果未连接或连接已断开则重新连接
        if (channel == null || !channel.isActive()) {
            connect();
        }
        
        try {
            // 步骤2：生成唯一的请求ID
            // 用于在异步环境中匹配请求和响应
            String requestId = generateRequestId();
            request.setRequestId(requestId);
            
            // 步骤3：创建RpcMessage消息对象
            // 将RpcRequest封装为RpcMessage，添加协议元数据
            RpcMessage message = new RpcMessage();
            message.setSerializerType(serializerType);  // 设置序列化类型
            message.setMessageType(RpcProtocol.MessageType.REQUEST);  // 标记为请求消息
            message.setRequestId(requestId);  // 设置请求ID
            message.setData(request);  // 设置消息体
            
            // 步骤4：创建CompletableFuture并注册到待处理请求Map
            // 这是异步RPC的关键：当前线程会等待这个Future完成
            CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();
            PENDING_REQUESTS.put(requestId, responseFuture);
            
            // 步骤5：异步发送请求
            // writeAndFlush()会将消息写入Channel并立即发送
            // addListener()添加监听器，监听发送结果
            channel.writeAndFlush(message).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    // 发送成功
                    logger.debug("Request sent successfully: {}", requestId);
                } else {
                    // 发送失败，异常完成Future
                    logger.error("Failed to send request: {}", requestId, future.cause());
                    responseFuture.completeExceptionally(future.cause());
                    // 从待处理Map中移除
                    PENDING_REQUESTS.remove(requestId);
                }
            });
            
            // 步骤6：等待响应（阻塞当前线程）
            // get(timeout)会阻塞等待，直到：
            // - 接收到响应：completePendingRequest被调用，Future被complete
            // - 超时：抛出TimeoutException
            // - 异常：抛出ExecutionException
            return responseFuture.get(30, TimeUnit.SECONDS);
            
        } catch (Exception e) {
            // 捕获所有异常（超时、中断、执行异常等）
            logger.error("Failed to send request", e);
            throw new RuntimeException("RPC call failed", e);
        }
    }
    
    /**
     * 完成待处理的请求 - 由Handler调用，完成Future
     * Complete pending request
     * 
     * 该方法由NettyRpcClientHandler在接收到响应时调用
     * 
     * 工作流程：
     * 1. 根据requestId从PENDING_REQUESTS中取出对应的Future
     * 2. 调用Future.complete(response)，唤醒等待的线程
     * 3. 等待的sendRequest方法继续执行并返回结果
     * 
     * 为什么是static方法？
     * - Handler是内部类，需要访问外部类的static字段
     * - PENDING_REQUESTS是static的，所有客户端实例共享
     * 
     * @param requestId 请求唯一标识
     * @param response RPC响应对象
     */
    public static void completePendingRequest(String requestId, RpcResponse response) {
        // 从Map中移除并获取Future
        // 使用remove而不是get，避免内存泄漏
        CompletableFuture<RpcResponse> future = PENDING_REQUESTS.remove(requestId);
        
        if (future != null) {
            // 完成Future，唤醒等待的线程
            future.complete(response);
        } else {
            // 警告：找不到对应的请求
            // 可能的原因：1.超时被清理 2.重复响应 3.requestId错误
            logger.warn("No pending request found for requestId: {}", requestId);
        }
    }
    
    /**
     * 关闭客户端并释放资源 - 优雅关闭
     * Close client and release resources
     * 
     * 关闭步骤：
     * 1. 关闭Channel，断开与服务器的连接
     * 2. 优雅关闭EventLoopGroup，等待任务完成
     * 
     * shutdownGracefully()的作用：
     * - 停止接收新任务
     * - 等待现有任务执行完成
     * - 释放线程资源
     * - 相比shutdown()更安全
     */
    public void close() {
        // 关闭Channel
        if (channel != null) {
            channel.close();
        }
        
        // 优雅关闭事件循环组
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }
        
        logger.info("Client closed");
    }
    
    /**
     * 生成唯一的请求ID
     * Generate unique request ID
     * 
     * ID组成：线程ID + 纳秒时间戳
     * - 线程ID：确保同一时刻不同线程生成的ID不同
     * - 纳秒时间戳：确保同一线程不同时刻生成的ID不同
     * 
     * 为什么使用纳秒而不是毫秒？
     * - 纳秒精度更高，避免高并发下的ID冲突
     * - nanoTime()比currentTimeMillis()更适合测量时间间隔
     * 
     * @return 唯一的请求ID字符串
     */
    private String generateRequestId() {
        return Thread.currentThread().getId() + "-" + System.nanoTime();
    }
    
    /**
     * 客户端处理器 - 处理服务器返回的响应消息
     * Client handler for processing responses
     * 
     * 该Handler负责处理从服务器接收到的响应消息
     * 
     * 继承关系：
     * - SimpleChannelInboundHandler<RpcMessage>: Netty提供的入站处理器基类
     * - 泛型参数RpcMessage: 只处理RpcMessage类型的消息
     * - 自动进行类型转换和消息释放
     * 
     * 工作流程：
     * 1. Netty的I/O线程接收到数据
     * 2. 经过Decoder解码为RpcMessage对象
     * 3. channelRead0方法被调用
     * 4. 根据requestId找到对应的Future
     * 5. 完成Future，唤醒等待的业务线程
     * 
     * 为什么是static内部类？
     * - 不需要访问外部类的实例字段
     * - 减少内存占用（不持有外部类引用）
     * - 访问static的PENDING_REQUESTS
     */
    private static class NettyRpcClientHandler extends SimpleChannelInboundHandler<RpcMessage> {
        
        private static final Logger logger = LoggerFactory.getLogger(NettyRpcClientHandler.class);
        
        /**
         * 处理接收到的消息 - Netty的I/O线程调用
         * Process received message
         * 
         * 该方法在Netty的I/O线程中执行，不是发送请求的业务线程
         * 
         * 处理步骤：
         * 1. 记录接收到响应的日志
         * 2. 判断消息类型是否为RESPONSE
         * 3. 提取RpcResponse对象
         * 4. 获取requestId（注意：从response中获取，不是message）
         * 5. 调用completePendingRequest完成Future
         * 
         * 注意事项：
         * - 该方法执行要快，不要阻塞I/O线程
         * - 复杂的业务逻辑应该在业务线程中处理
         * - 异常会被exceptionCaught捕获
         * 
         * @param ctx Channel上下文
         * @param msg 接收到的RpcMessage对象
         */
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {
            logger.debug("Received response: {}", msg);
            
            // 判断消息类型是否为响应
            if (msg.getMessageType() == RpcProtocol.MessageType.RESPONSE) {
                // 提取响应数据
                RpcResponse response = (RpcResponse) msg.getData();
                
                // 获取请求ID
                // 注意：从response对象中获取，而不是从message中获取
                // 因为requestId是业务层的概念，存储在RpcResponse中
                String requestId = response.getRequestId();
                
                // 完成待处理的请求
                // 这会唤醒等待该响应的业务线程
                NettyRpcClient.completePendingRequest(requestId, response);
            }
        }
        
        /**
         * 异常处理 - 捕获处理过程中的异常
         * Exception handling
         * 
         * 当Handler处理消息时发生异常，该方法会被调用
         * 
         * 常见异常场景：
         * - 网络连接断开
         * - 消息解码失败
         * - 业务逻辑异常
         * 
         * 处理策略：
         * - 记录错误日志
         * - 关闭Channel连接
         * - 避免异常传播到Netty框架
         * 
         * @param ctx Channel上下文
         * @param cause 异常对象
         */
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Exception in client handler", cause);
            // 关闭连接
            ctx.close();
        }
    }
}

