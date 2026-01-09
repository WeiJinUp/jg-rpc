package com.jinguan.rpc.client.discovery;

import com.jinguan.rpc.api.registry.ServiceDiscovery;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于Zookeeper的服务发现实现
 * Zookeeper-based Service Discovery
 * 
 * 该实现从Zookeeper中读取服务提供者的地址列表
 * 
 * 发现流程：
 * 1. 构建服务路径：/jg-rpc/{serviceName}/providers
 * 2. 检查路径是否存在
 * 3. 获取providers目录下的所有子节点
 * 4. 解析节点名称（格式：ip:port）为InetSocketAddress
 * 5. 返回地址列表
 * 
 * 节点结构：
 * <pre>
 * /jg-rpc/com.jinguan.rpc.api.HelloService/providers
 *   ├── 192.168.1.100:9000  (服务实例1)
 *   ├── 192.168.1.101:9000  (服务实例2)
 *   └── 192.168.1.102:9000  (服务实例3)
 * </pre>
 * 
 * 性能优化建议：
 * - 可以缓存服务列表，减少Zookeeper查询
 * - 可以监听节点变化，实时更新服务列表
 * - 可以使用PathChildrenCache实现自动刷新
 * 
 * 与负载均衡的配合：
 * - discoverAll()返回所有地址
 * - 由LoadBalancer选择一个地址
 * - 实现高可用和负载均衡
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class ZookeeperServiceDiscovery implements ServiceDiscovery {
    
    private static final Logger logger = LoggerFactory.getLogger(ZookeeperServiceDiscovery.class);
    
    /**
     * Zookeeper根路径 - 与服务注册保持一致
     */
    private static final String ZK_ROOT_PATH = "/jg-rpc";
    
    /**
     * Curator客户端 - 用于查询Zookeeper
     */
    private final CuratorFramework zkClient;
    
    /**
     * 构造函数 - 创建并启动Zookeeper客户端
     * 
     * 配置与服务注册端相同，确保能正确连接
     * 
     * @param zkAddress Zookeeper连接地址
     */
    public ZookeeperServiceDiscovery(String zkAddress) {
        this.zkClient = CuratorFrameworkFactory.builder()
                .connectString(zkAddress)
                .sessionTimeoutMs(5000)
                .connectionTimeoutMs(3000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        
        zkClient.start();
        logger.info("Zookeeper client started for discovery: {}", zkAddress);
    }
    
    /**
     * 发现单个服务实例 - 返回第一个可用地址
     * Discover a single service instance
     * 
     * 实现方式：
     * - 调用discoverAll()获取所有地址
     * - 返回第一个地址
     * 
     * 注意：该方法没有负载均衡，建议使用discoverAll() + LoadBalancer
     * 
     * @param serviceName 服务名称
     * @return 服务地址，如果未找到则返回null
     */
    @Override
    public InetSocketAddress discover(String serviceName) {
        List<InetSocketAddress> addresses = discoverAll(serviceName);
        if (addresses == null || addresses.isEmpty()) {
            logger.warn("No available service found: {}", serviceName);
            return null;
        }
        
        // 返回第一个地址（实际应该使用负载均衡器选择）
        return addresses.get(0);
    }
    
    /**
     * 发现所有服务实例 - 从Zookeeper读取所有提供者地址
     * Discover all instances
     * 
     * 发现流程（详细步骤）：
     * 1. 构建providers路径
     * 2. 检查路径是否存在
     * 3. 获取providers目录下的所有子节点名称
     * 4. 遍历节点名称，解析为InetSocketAddress
     * 5. 返回地址列表
     * 
     * 节点名称格式：
     * - 格式：ip:port（如：192.168.1.100:9000）
     * - 与服务注册时的格式保持一致
     * 
     * 异常处理：
     * - 如果服务不存在，返回空列表
     * - 如果解析失败，跳过该节点
     * - 如果查询异常，记录日志并返回空列表
     * 
     * @param serviceName 服务名称
     * @return 服务地址列表，如果未找到则返回空列表
     */
    @Override
    public List<InetSocketAddress> discoverAll(String serviceName) {
        List<InetSocketAddress> addresses = new ArrayList<>();
        
        try {
            // 步骤1：构建providers路径
            String providersPath = ZK_ROOT_PATH + "/" + serviceName + "/providers";
            
            // 步骤2：检查服务是否存在
            if (zkClient.checkExists().forPath(providersPath) == null) {
                logger.warn("Service not found in Zookeeper: {}", serviceName);
                return addresses;  // 返回空列表
            }
            
            // 步骤3：获取所有提供者节点名称
            // 节点名称格式：ip:port
            List<String> providerNodes = zkClient.getChildren().forPath(providersPath);
            
            // 步骤4：解析节点名称为地址
            for (String node : providerNodes) {
                // 解析格式：ip:port
                String[] parts = node.split(":");
                if (parts.length == 2) {
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    addresses.add(new InetSocketAddress(host, port));
                } else {
                    // 格式错误，跳过
                    logger.warn("Invalid node format: {}", node);
                }
            }
            
            logger.info("Discovered {} instances for service: {}", addresses.size(), serviceName);
            
        } catch (Exception e) {
            // 步骤5：异常处理
            logger.error("Failed to discover service: {}", serviceName, e);
        }
        
        return addresses;
    }
    
    /**
     * 关闭Zookeeper客户端
     * Close Zookeeper client
     */
    public void close() {
        if (zkClient != null) {
            zkClient.close();
            logger.info("Zookeeper discovery client closed");
        }
    }
}

