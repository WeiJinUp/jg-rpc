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
 * 基于Netty的RPC服务器 - 高性能事件驱动服务器
 * Netty-based RPC Server
 * 
 * 该服务器使用Netty框架实现高性能的网络通信
 * 
 * 核心特性：
 * 1. 事件驱动模型：基于Netty的Reactor模式
 * 2. 异步非阻塞I/O：高并发处理能力
 * 3. 主从Reactor模式：BossGroup接收连接，WorkerGroup处理I/O
 * 4. Pipeline设计：责任链模式处理请求
 * 5. 空闲连接检测：自动断开空闲连接
 * 6. 优雅关闭：正确释放资源
 * 
 * Netty线程模型（主从Reactor）：
 * <pre>
 *                    ┌─────────────┐
 *    客户端连接 ───→  │  BossGroup  │ ──→ 接收连接，创建Channel
 *                    └──────┬──────┘
 *                           │ 注册Channel
 *                           ↓
 *                    ┌─────────────┐
 *                    │ WorkerGroup │ ──→ 处理I/O读写
 *                    └──────┬──────┘
 *                           │
 *                           ↓
 *                    ┌─────────────┐
 *                    │  Pipeline   │ ──→ 处理业务逻辑
 *                    └─────────────┘
 * </pre>
 * 
 * 相比第一阶段Socket服务器的优势：
 * - 性能更高：事件驱动，少量线程处理大量连接
 * - 可扩展性强：Pipeline设计易于扩展功能
 * - 内存优化：零拷贝、内存池等优化
 * - 协议支持好：自带多种编解码器
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class NettyRpcServer {
    
    private static final Logger logger = LoggerFactory.getLogger(NettyRpcServer.class);
    
    /**
     * 默认服务端口
     */
    private static final int DEFAULT_PORT = 9000;
    
    /**
     * 服务注册表 - 存储接口名到服务实现的映射
     * Key: 接口全限定名
     * Value: 服务实现实例
     */
    private final Map<String, Object> serviceMap = new HashMap<>();
    
    /**
     * 服务器监听端口
     */
    private final int port;
    
    /**
     * Boss事件循环组 - 负责接收客户端连接
     * Boss event loop group - handles client connections
     * 
     * BossGroup的职责：
     * 1. 监听ServerSocket的ACCEPT事件
     * 2. 接收新的客户端连接
     * 3. 创建SocketChannel
     * 4. 将Channel注册到WorkerGroup
     * 
     * 通常只需要1个线程即可
     */
    private EventLoopGroup bossGroup;
    
    /**
     * Worker事件循环组 - 负责处理I/O操作
     * Worker event loop group - handles I/O operations
     * 
     * WorkerGroup的职责：
     * 1. 处理Channel的READ事件（接收数据）
     * 2. 处理Channel的WRITE事件（发送数据）
     * 3. 执行Pipeline中的Handler
     * 
     * 线程数通常为：CPU核心数 * 2
     */
    private EventLoopGroup workerGroup;
    
    /**
     * 服务器Channel
     * 代表服务器的监听Socket
     */
    private Channel serverChannel;
    
    /**
     * 无参构造函数 - 使用默认端口9000
     */
    public NettyRpcServer() {
        this(DEFAULT_PORT);
    }
    
    /**
     * 有参构造函数 - 指定服务端口
     * 
     * @param port 服务器监听端口
     */
    public NettyRpcServer(int port) {
        this.port = port;
    }
    
    /**
     * 注册服务实现 - 将服务实例注册到服务器
     * Register a service implementation
     * 
     * 注册流程：
     * 1. 验证服务实例不为空
     * 2. 获取服务实现的所有接口
     * 3. 验证至少实现一个接口
     * 4. 遍历所有接口，逐个注册到serviceMap
     * 5. 记录注册日志
     * 
     * 与第一阶段的区别：
     * - 第一阶段：只注册第一个接口
     * - 第二阶段：注册所有接口
     * 
     * 优点：一个服务实现类可以实现多个接口，客户端可以通过不同的接口调用
     * 
     * @param service 服务实现实例
     * @throws IllegalArgumentException 如果服务为null或未实现接口
     */
    public void register(Object service) {
        // 步骤1：验证服务实例
        if (service == null) {
            throw new IllegalArgumentException("Service cannot be null");
        }
        
        // 步骤2：获取所有接口
        Class<?>[] interfaces = service.getClass().getInterfaces();
        
        // 步骤3：验证接口数量
        if (interfaces.length == 0) {
            throw new IllegalArgumentException("Service must implement at least one interface");
        }
        
        // 步骤4：注册所有接口
        for (Class<?> intf : interfaces) {
            String interfaceName = intf.getName();
            serviceMap.put(interfaceName, service);
            logger.info("Registered service: {}", interfaceName);
        }
    }
    
    /**
     * 根据接口名获取服务实例
     * Get service instance by interface name
     * 
     * 该方法由Handler调用，用于处理RPC请求时查找服务实例
     * 
     * @param interfaceName 接口全限定名
     * @return 服务实现实例，如果未注册则返回null
     */
    public Object getService(String interfaceName) {
        return serviceMap.get(interfaceName);
    }
    
    /**
     * 启动Netty RPC服务器 - 核心启动方法
     * Start the Netty RPC server
     * 
     * 该方法配置并启动Netty服务器，是服务器的核心方法
     * 
     * 启动步骤（详细）：
     * 1. 创建Boss和Worker事件循环组
     * 2. 创建ServerBootstrap并配置参数
     * 3. 设置Channel类型和选项
     * 4. 配置Pipeline处理链
     * 5. 绑定端口并启动监听
     * 6. 等待服务器关闭
     * 7. 优雅关闭资源
     */
    public void start() {
        // 步骤1：创建事件循环组
        // BossGroup：1个线程，负责接收连接
        bossGroup = new NioEventLoopGroup(1);
        // WorkerGroup：默认CPU核心数*2个线程，负责I/O处理
        workerGroup = new NioEventLoopGroup();
        
        try {
            // 步骤2：创建服务器启动器
            ServerBootstrap bootstrap = new ServerBootstrap();
            
            // 步骤3：配置ServerBootstrap
            bootstrap.group(bossGroup, workerGroup)  // 设置主从线程组
                    .channel(NioServerSocketChannel.class)  // 使用NIO ServerSocket Channel
                    
                    // 配置ServerSocket选项
                    .option(ChannelOption.SO_BACKLOG, 128)  // 连接队列大小
                    
                    // 配置SocketChannel选项（childOption针对接收到的客户端连接）
                    .childOption(ChannelOption.SO_KEEPALIVE, true)  // 启用TCP心跳检测
                    .childOption(ChannelOption.TCP_NODELAY, true)   // 禁用Nagle算法
                    
                    // 配置ServerSocket的Handler（用于记录日志）
                    .handler(new LoggingHandler(LogLevel.INFO))
                    
                    // 步骤4：配置SocketChannel的Pipeline
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        /**
                         * 初始化Channel的Pipeline
                         * 该方法在每个新连接建立时被调用
                         */
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // 1. 空闲检测Handler
                            // 如果30秒内没有读事件，触发IdleStateEvent
                            // 服务器会在Handler中关闭这个空闲连接
                            pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            
                            // 2. 解码器 - 将字节流解码为RpcMessage
                            pipeline.addLast(new RpcDecoder());
                            
                            // 3. 编码器 - 将RpcMessage编码为字节流
                            pipeline.addLast(new RpcEncoder());
                            
                            // 4. 业务逻辑Handler - 处理RPC请求
                            // 传入NettyRpcServer实例，以便Handler可以获取服务实例
                            pipeline.addLast(new NettyRpcServerHandler(NettyRpcServer.this));
                        }
                    });
            
            // 步骤5：绑定端口并启动服务器
            // bind()返回ChannelFuture，代表异步绑定操作
            // sync()等待绑定完成
            ChannelFuture future = bootstrap.bind(port).sync();
            serverChannel = future.channel();
            
            // 打印启动成功信息
            logger.info("========================================");
            logger.info("  Netty RPC Server started");
            logger.info("  Port: {}", port);
            logger.info("  Registered services: {}", serviceMap.size());
            logger.info("========================================");
            
            // 步骤6：等待服务器Channel关闭
            // 这是一个阻塞操作，服务器会一直运行
            // 直到serverChannel被关闭（调用shutdown或异常）
            serverChannel.closeFuture().sync();
            
        } catch (InterruptedException e) {
            // 服务器被中断
            logger.error("Server interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            // 步骤7：无论如何都要优雅关闭
            shutdown();
        }
    }
    
    /**
     * 优雅关闭服务器 - 释放所有资源
     * Shutdown the server gracefully
     * 
     * 关闭步骤：
     * 1. 关闭ServerChannel，停止接收新连接
     * 2. 优雅关闭BossGroup，等待任务完成
     * 3. 优雅关闭WorkerGroup，等待任务完成
     * 
     * shutdownGracefully()的作用：
     * - 停止接收新任务
     * - 等待现有任务执行完成
     * - 释放线程资源
     * - 比shutdown()更安全
     */
    public void shutdown() {
        logger.info("Shutting down Netty RPC server...");
        
        // 步骤1：关闭ServerChannel
        if (serverChannel != null) {
            serverChannel.close();
        }
        
        // 步骤2：优雅关闭BossGroup
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        
        // 步骤3：优雅关闭WorkerGroup
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        
        logger.info("Netty RPC server stopped");
    }
}

