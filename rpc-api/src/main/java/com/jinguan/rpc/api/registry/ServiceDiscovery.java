package com.jinguan.rpc.api.registry;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 服务发现接口 - 定义服务发现的标准规范
 * Service Discovery Interface
 * 
 * 该接口定义了服务发现的核心功能，用于从注册中心获取服务提供者的地址
 * 
 * 核心功能：
 * 1. 单实例发现：获取一个可用的服务实例地址
 * 2. 全量发现：获取所有可用的服务实例地址
 * 
 * 设计模式：
 * - 策略模式：不同的注册中心实现该接口
 * - 与ServiceRegistry分离：遵循接口隔离原则
 * 
 * 使用场景：
 * - 客户端启动时：发现服务地址列表
 * - RPC调用前：通过负载均衡选择服务实例
 * - 服务变更时：监听注册中心变化，更新服务列表
 * 
 * 实现类：
 * - ZookeeperServiceDiscovery: 基于Zookeeper的实现
 * - 可以扩展：NacosServiceDiscovery、ConsulServiceDiscovery等
 * 
 * 与负载均衡的关系：
 * - discover()：返回单个地址，通常内部已做负载均衡
 * - discoverAll()：返回所有地址，由调用方进行负载均衡
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public interface ServiceDiscovery {
    
    /**
     * 发现单个服务实例 - 获取一个可用的服务地址
     * Discover a single service instance
     * 
     * 该方法用于获取单个服务实例，通常用于：
     * - 简单的服务调用场景
     * - 不需要负载均衡的场景
     * 
     * 实现建议：
     * - 如果只有一个实例，直接返回
     * - 如果有多个实例，可以使用简单的负载均衡策略（如随机、轮询）
     * - 如果无可用实例，返回null
     * 
     * 注意事项：
     * - 返回null表示服务不可用，调用方需要处理
     * - 建议使用discoverAll() + LoadBalancer的方式，更灵活
     * 
     * @param serviceName 服务名称（通常是接口的全限定名）
     * @return 服务地址，如果未找到则返回null
     */
    InetSocketAddress discover(String serviceName);
    
    /**
     * 发现所有服务实例 - 获取所有可用的服务地址列表
     * Discover all instances of a service
     * 
     * 该方法用于获取服务的所有实例，通常用于：
     * - 需要负载均衡的场景
     * - 需要故障转移的场景
     * - 需要服务列表缓存的场景
     * 
     * 返回列表的特点：
     * - 包含所有可用的服务实例地址
     * - 列表顺序可能不固定
     * - 列表可能为空（服务不可用）
     * 
     * 使用流程：
     * 1. 调用discoverAll()获取所有地址
     * 2. 使用LoadBalancer选择一个地址
     * 3. 使用选中的地址创建RPC客户端
     * 
     * 性能考虑：
     * - 可以缓存服务列表，减少注册中心查询
     * - 可以监听注册中心变化，实时更新列表
     * 
     * @param serviceName 服务名称
     * @return 服务地址列表，如果未找到则返回空列表（不会返回null）
     */
    List<InetSocketAddress> discoverAll(String serviceName);
}

