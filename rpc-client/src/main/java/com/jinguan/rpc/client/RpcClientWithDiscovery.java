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
 * 带服务发现和负载均衡的RPC客户端管理器
 * RPC Client with Service Discovery and Load Balancing
 * 
 * 该类是第三阶段的核心客户端组件，整合了服务发现和负载均衡功能
 * 
 * 核心功能：
 * 1. 服务发现：从注册中心获取服务地址列表
 * 2. 负载均衡：从多个服务实例中选择一个
 * 3. 客户端缓存：复用NettyRpcClient实例，避免重复创建
 * 4. 连接管理：统一管理所有客户端连接
 * 
 * 工作流程：
 * 1. 客户端调用getClient(serviceName)
 * 2. 通过ServiceDiscovery发现所有服务实例
 * 3. 使用LoadBalancer选择一个实例
 * 4. 从缓存中获取或创建NettyRpcClient
 * 5. 返回客户端实例供RPC调用使用
 * 
 * 客户端缓存机制：
 * - Key: 服务地址（host:port）
 * - Value: NettyRpcClient实例
 * - 复用连接，提高性能
 * - 避免频繁创建和销毁连接
 * 
 * 设计模式：
 * - 门面模式：封装服务发现和负载均衡的复杂性
 * - 工厂模式：按需创建客户端实例
 * - 缓存模式：复用客户端连接
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class RpcClientWithDiscovery {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcClientWithDiscovery.class);
    
    /**
     * 服务发现组件 - 从注册中心获取服务地址
     */
    private final ServiceDiscovery serviceDiscovery;
    
    /**
     * 负载均衡器 - 从多个实例中选择一个
     */
    private final LoadBalancer loadBalancer;
    
    /**
     * 客户端缓存 - 按地址缓存NettyRpcClient实例
     * Cache of NettyRpcClient instances by address
     * 
     * Key: 服务地址字符串（格式：host:port）
     * Value: NettyRpcClient实例
     * 
     * 为什么需要缓存？
     * - 避免为同一个地址重复创建客户端
     * - 复用连接，提高性能
     * - 减少资源消耗
     * 
     * 线程安全：
     * - computeIfAbsent是线程安全的
     * - 但HashMap本身不是线程安全的
     * - 建议使用ConcurrentHashMap（本实现简化处理）
     */
    private final Map<String, NettyRpcClient> clientCache = new HashMap<>();
    
    /**
     * 构造函数 - 使用默认的轮询负载均衡器
     * Constructor with default round robin load balancer
     * 
     * @param serviceDiscovery 服务发现组件
     */
    public RpcClientWithDiscovery(ServiceDiscovery serviceDiscovery) {
        this(serviceDiscovery, new RoundRobinLoadBalancer());
    }
    
    /**
     * 构造函数 - 指定负载均衡器
     * Constructor with custom load balancer
     * 
     * @param serviceDiscovery 服务发现组件
     * @param loadBalancer 负载均衡器（轮询、随机、一致性哈希等）
     */
    public RpcClientWithDiscovery(ServiceDiscovery serviceDiscovery, LoadBalancer loadBalancer) {
        this.serviceDiscovery = serviceDiscovery;
        this.loadBalancer = loadBalancer;
        logger.info("RPC client initialized with discovery and load balancing");
    }
    
    /**
     * 获取RPC客户端 - 自动发现服务并选择实例
     * Get RPC client for the given service
     * 
     * 获取流程（详细步骤）：
     * 1. 通过ServiceDiscovery发现所有服务实例
     * 2. 验证是否有可用实例
     * 3. 使用LoadBalancer选择一个实例
     * 4. 构建缓存Key（host:port）
     * 5. 从缓存获取或创建NettyRpcClient
     * 6. 返回客户端实例
     * 
     * 为什么每次调用都重新发现？
     * - 服务实例可能动态变化（上线/下线）
     * - 保证获取最新的服务列表
     * - 可以优化为定时刷新 + 缓存
     * 
     * 客户端复用：
     * - 相同地址的请求复用同一个客户端
     * - 减少连接数，提高性能
     * - 客户端内部维护长连接
     * 
     * @param serviceName 服务名称（接口全限定名）
     * @return NettyRpcClient实例，用于RPC调用
     * @throws RuntimeException 如果服务不可用
     */
    public NettyRpcClient getClient(String serviceName) {
        // 步骤1：发现所有可用实例
        List<InetSocketAddress> addresses = serviceDiscovery.discoverAll(serviceName);
        
        // 步骤2：验证服务可用性
        if (addresses == null || addresses.isEmpty()) {
            throw new RuntimeException("No available service found: " + serviceName);
        }
        
        // 步骤3：使用负载均衡器选择一个实例
        InetSocketAddress selected = loadBalancer.select(addresses, serviceName);
        logger.debug("Selected server: {} for service: {}", selected, serviceName);
        
        // 步骤4-5：获取或创建客户端
        String key = selected.getHostString() + ":" + selected.getPort();
        return clientCache.computeIfAbsent(key, k -> {
            // 如果缓存中没有，创建新的客户端
            logger.info("Creating new client for: {}", selected);
            return new NettyRpcClient(selected.getHostString(), selected.getPort());
        });
    }
    
    /**
     * 关闭所有客户端并释放资源
     * Close all clients and release resources
     * 
     * 关闭流程：
     * 1. 遍历所有缓存的客户端
     * 2. 逐个关闭客户端连接
     * 3. 清空缓存
     * 
     * 使用场景：
     * - 应用程序关闭时
     * - 资源清理时
     * - 优雅停机时
     */
    public void close() {
        logger.info("Closing all RPC clients...");
        
        // 关闭所有客户端
        for (NettyRpcClient client : clientCache.values()) {
            try {
                client.close();
            } catch (Exception e) {
                // 即使部分关闭失败，也要继续关闭其他客户端
                logger.error("Error closing client", e);
            }
        }
        
        // 清空缓存
        clientCache.clear();
        logger.info("All clients closed");
    }
}

