package com.jinguan.rpc.server.registry;

import com.jinguan.rpc.api.registry.ServiceRegistry;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于Zookeeper的服务注册实现
 * Zookeeper-based Service Registry
 * 
 * 该实现使用Zookeeper作为注册中心，将服务注册为临时节点
 * 
 * Zookeeper节点结构：
 * <pre>
 * /jg-rpc                          (根路径)
 *   └── com.jinguan.rpc.api.HelloService  (服务名)
 *       └── providers              (提供者目录)
 *           ├── 192.168.1.100:9000  (服务实例1 - 临时节点)
 *           ├── 192.168.1.101:9000  (服务实例2 - 临时节点)
 *           └── 192.168.1.102:9000  (服务实例3 - 临时节点)
 * </pre>
 * 
 * 核心特性：
 * 1. 临时节点（EPHEMERAL）：服务断开时自动删除，实现自动注销
 * 2. 持久化父节点（PERSISTENT）：服务目录永久存在
 * 3. 路径管理：记录所有注册路径，便于批量注销
 * 4. 重试机制：网络异常时自动重试
 * 
 * 为什么使用临时节点？
 * - 服务异常退出时，Zookeeper会自动删除节点
 * - 避免注册中心出现"僵尸"服务
 * - 客户端能及时发现服务不可用
 * 
 * 使用Curator框架的原因：
 * - 简化Zookeeper操作
 * - 提供连接重试、会话管理等高级特性
 * - 比原生Zookeeper API更易用
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class ZookeeperServiceRegistry implements ServiceRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperServiceRegistry.class);
    
    /**
     * Zookeeper根路径 - 所有RPC服务都注册在此路径下
     * Zookeeper root path for RPC services
     * 
     * 路径设计：
     * - 使用统一的根路径，便于管理
     * - 可以按环境区分：/jg-rpc-dev、/jg-rpc-prod
     */
    private static final String ZK_ROOT_PATH = "/jg-rpc";
    
    /**
     * Curator客户端 - Zookeeper的高级客户端
     * Curator client
     * 
     * Curator是Netflix开源的Zookeeper客户端框架，提供：
     * - 自动重连
     * - 会话管理
     * - 节点监听
     * - 分布式锁等高级功能
     */
    private final CuratorFramework zkClient;
    
    /**
     * 已注册的服务路径列表 - 用于清理
     * Registered service paths (for cleanup)
     * 
     * 记录所有注册的路径，在优雅关闭时批量删除
     * 使用List而不是Set，因为路径是唯一的
     */
    private final List<String> registeredPaths = new ArrayList<>();
    
    /**
     * 构造函数 - 创建并启动Zookeeper客户端
     * Constructor
     * 
     * 配置步骤：
     * 1. 设置Zookeeper连接地址
     * 2. 设置会话超时时间（5秒）
     * 3. 设置连接超时时间（3秒）
     * 4. 设置重试策略（指数退避，最多重试3次）
     * 5. 启动客户端
     * 
     * 重试策略说明：
     * - ExponentialBackoffRetry：指数退避重试
     * - 初始间隔1秒，最多重试3次
     * - 适合网络抖动场景
     * 
     * @param zkAddress Zookeeper连接地址（格式：host:port，如：localhost:2181）
     */
    public ZookeeperServiceRegistry(String zkAddress) {
        // 步骤1-4：创建Curator客户端并配置参数
        this.zkClient = CuratorFrameworkFactory.builder()
                .connectString(zkAddress)  // Zookeeper地址
                .sessionTimeoutMs(5000)   // 会话超时5秒
                .connectionTimeoutMs(3000) // 连接超时3秒
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))  // 重试策略
                .build();
        
        // 步骤5：启动客户端
        zkClient.start();
        logger.info("Zookeeper client started: {}", zkAddress);
    }
    
    /**
     * 注册服务 - 在Zookeeper中创建服务节点
     * Register a service
     * 
     * 注册流程（详细步骤）：
     * 1. 构建服务路径：/jg-rpc/{serviceName}/providers/{ip:port}
     * 2. 检查并创建服务父路径（PERSISTENT节点）
     * 3. 检查并创建providers目录（PERSISTENT节点）
     * 4. 创建服务实例节点（EPHEMERAL节点）
     * 5. 记录路径到registeredPaths，用于后续清理
     * 
     * 节点类型说明：
     * - PERSISTENT（持久节点）：服务目录永久存在，即使服务全部下线
     * - EPHEMERAL（临时节点）：服务实例节点，连接断开时自动删除
     * 
     * 为什么使用EPHEMERAL节点？
     * - 服务异常退出时，Zookeeper会自动删除节点
     * - 客户端能及时发现服务不可用
     * - 避免手动注销的复杂性
     * 
     * 路径格式：
     * - 服务路径：/jg-rpc/com.jinguan.rpc.api.HelloService
     * - 提供者目录：/jg-rpc/com.jinguan.rpc.api.HelloService/providers
     * - 实例节点：/jg-rpc/com.jinguan.rpc.api.HelloService/providers/192.168.1.100:9000
     * 
     * @param serviceName 服务名称（接口全限定名）
     * @param address 服务地址（IP:Port）
     * @throws RuntimeException 如果注册失败
     */
    @Override
    public void register(String serviceName, InetSocketAddress address) {
        try {
            // 步骤1：构建路径
            String servicePath = ZK_ROOT_PATH + "/" + serviceName;
            String addressStr = address.getHostString() + ":" + address.getPort();
            String providerPath = servicePath + "/providers/" + addressStr;
            
            // 步骤2：创建服务父路径（如果不存在）
            // 使用PERSISTENT节点，确保目录永久存在
            if (zkClient.checkExists().forPath(servicePath) == null) {
                zkClient.create()
                        .creatingParentsIfNeeded()  // 自动创建父路径
                        .withMode(CreateMode.PERSISTENT)  // 持久节点
                        .forPath(servicePath);
            }
            
            // 步骤3：创建providers目录（如果不存在）
            if (zkClient.checkExists().forPath(servicePath + "/providers") == null) {
                zkClient.create()
                        .withMode(CreateMode.PERSISTENT)  // 持久节点
                        .forPath(servicePath + "/providers");
            }
            
            // 步骤4：创建服务实例节点（EPHEMERAL临时节点）
            // 如果节点已存在，说明之前注册过，跳过
            if (zkClient.checkExists().forPath(providerPath) == null) {
                zkClient.create()
                        .withMode(CreateMode.EPHEMERAL)  // 临时节点
                        .forPath(providerPath);
                
                // 步骤5：记录路径，用于后续清理
                registeredPaths.add(providerPath);
                logger.info("Service registered: {} -> {}", serviceName, addressStr);
            }
            
        } catch (Exception e) {
            logger.error("Failed to register service: {}", serviceName, e);
            throw new RuntimeException("Service registration failed", e);
        }
    }
    
    /**
     * 注销服务 - 从Zookeeper删除指定的服务节点
     * Unregister a service
     * 
     * 注销流程：
     * 1. 构建服务实例路径
     * 2. 检查节点是否存在
     * 3. 删除节点
     * 4. 从registeredPaths中移除记录
     * 
     * 注意事项：
     * - 如果节点不存在，说明可能已被自动删除（连接断开）
     * - 即使删除失败，也不应该抛出异常，避免影响关闭流程
     * - 注销后，客户端会通过监听机制发现服务不可用
     * 
     * @param serviceName 服务名称
     * @param address 服务地址
     */
    @Override
    public void unregister(String serviceName, InetSocketAddress address) {
        try {
            // 构建路径
            String addressStr = address.getHostString() + ":" + address.getPort();
            String providerPath = ZK_ROOT_PATH + "/" + serviceName + "/providers/" + addressStr;
            
            // 检查并删除节点
            if (zkClient.checkExists().forPath(providerPath) != null) {
                zkClient.delete().forPath(providerPath);
                registeredPaths.remove(providerPath);
                logger.info("Service unregistered: {} -> {}", serviceName, addressStr);
            }
            
        } catch (Exception e) {
            // 注销失败不应该影响关闭流程
            logger.error("Failed to unregister service: {}", serviceName, e);
        }
    }
    
    /**
     * 批量注销服务 - 注销指定地址的所有服务
     * Unregister all services
     * 
     * 注销流程：
     * 1. 复制registeredPaths列表（避免并发修改异常）
     * 2. 遍历所有已注册的路径
     * 3. 逐个删除节点
     * 4. 清空registeredPaths列表
     * 
     * 为什么需要复制列表？
     * - 在遍历过程中删除元素会导致ConcurrentModificationException
     * - 先复制再遍历，避免并发问题
     * 
     * 使用场景：
     * - 服务关闭时，一次性清理所有注册信息
     * - 优雅停机流程的一部分
     * 
     * @param address 服务地址
     */
    @Override
    public void unregisterAll(InetSocketAddress address) {
        logger.info("Unregistering all services...");
        
        // 步骤1：复制列表，避免并发修改异常
        List<String> pathsToRemove = new ArrayList<>(registeredPaths);
        
        // 步骤2-3：遍历并删除所有节点
        for (String path : pathsToRemove) {
            try {
                if (zkClient.checkExists().forPath(path) != null) {
                    zkClient.delete().forPath(path);
                }
            } catch (Exception e) {
                // 即使部分删除失败，也要继续删除其他节点
                logger.error("Failed to delete path: {}", path, e);
            }
        }
        
        // 步骤4：清空列表
        registeredPaths.clear();
        logger.info("All services unregistered");
    }
    
    /**
     * 关闭Zookeeper客户端 - 释放资源
     * Close Zookeeper client
     * 
     * 关闭时会自动删除所有EPHEMERAL节点
     * 这是Zookeeper临时节点的特性
     */
    public void close() {
        if (zkClient != null) {
            zkClient.close();
            logger.info("Zookeeper client closed");
        }
    }
}

