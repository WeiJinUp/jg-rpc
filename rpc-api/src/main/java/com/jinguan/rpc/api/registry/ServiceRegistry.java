package com.jinguan.rpc.api.registry;

import java.net.InetSocketAddress;

/**
 * 服务注册接口 - 定义服务注册的标准规范
 * Service Registry Interface
 * 
 * 该接口定义了服务注册的核心功能，用于将服务提供者的信息注册到注册中心
 * 
 * 核心功能：
 * 1. 服务注册：将服务实例的地址注册到注册中心
 * 2. 服务注销：从注册中心移除服务实例
 * 3. 批量注销：注销指定地址的所有服务
 * 
 * 设计模式：
 * - 策略模式：不同的注册中心（Zookeeper、Nacos、Consul等）实现该接口
 * - 接口隔离：只定义注册相关功能，与发现功能分离
 * 
 * 使用场景：
 * - 服务启动时：调用register()注册服务
 * - 服务关闭时：调用unregister()或unregisterAll()注销服务
 * - 优雅停机：在JVM关闭钩子中调用unregisterAll()
 * 
 * 实现类：
 * - ZookeeperServiceRegistry: 基于Zookeeper的实现
 * - 可以扩展：NacosServiceRegistry、ConsulServiceRegistry等
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public interface ServiceRegistry {
    
    /**
     * 注册服务 - 将服务实例地址注册到注册中心
     * Register a service with the given address
     * 
     * 注册流程：
     * 1. 服务提供者启动后，调用该方法注册服务
     * 2. 注册中心存储服务名到地址的映射关系
     * 3. 客户端可以通过服务名从注册中心获取服务地址
     * 
     * 注册信息通常包括：
     * - 服务名称：通常是接口的全限定名
     * - 服务地址：IP地址和端口号
     * - 元数据：版本号、权重、标签等（可选）
     * 
     * 注意事项：
     * - 服务名应该是唯一的，通常使用接口全限定名
     * - 地址应该是可访问的，客户端会使用该地址连接
     * - 注册中心通常使用临时节点，服务断开时自动删除
     * 
     * @param serviceName 服务名称（通常是接口的全限定名，如：com.jinguan.rpc.api.HelloService）
     * @param address 服务提供者的地址（IP:Port）
     */
    void register(String serviceName, InetSocketAddress address);
    
    /**
     * 注销服务 - 从注册中心移除指定的服务实例
     * Unregister a service
     * 
     * 注销场景：
     * 1. 服务正常关闭时，主动注销
     * 2. 服务实例下线时，清理注册信息
     * 3. 服务迁移时，注销旧地址
     * 
     * 注意事项：
     * - 如果使用临时节点，服务断开时会自动删除，但仍建议主动注销
     * - 注销失败不应该影响服务关闭流程
     * 
     * @param serviceName 服务名称
     * @param address 要注销的服务地址
     */
    void unregister(String serviceName, InetSocketAddress address);
    
    /**
     * 批量注销服务 - 注销指定地址的所有服务
     * Unregister all services at the given address
     * 
     * 使用场景：
     * - 服务实例关闭时，该实例提供的所有服务都需要注销
     * - 优雅停机时，一次性清理所有注册信息
     * 
     * 实现建议：
     * - 遍历该地址注册的所有服务，逐个注销
     * - 即使部分注销失败，也要继续注销其他服务
     * - 最后清理本地缓存
     * 
     * @param address 服务提供者的地址
     */
    void unregisterAll(InetSocketAddress address);
}

