package com.jinguan.rpc.client.loadbalance;

import com.jinguan.rpc.api.loadbalance.LoadBalancer;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;

/**
 * 随机负载均衡器 - 随机选择服务实例
 * Random Load Balancer
 * 
 * 该负载均衡器使用随机算法选择服务实例
 * 
 * 工作原理：
 * 1. 生成一个随机数
 * 2. 使用随机数对服务数量取模，得到索引
 * 3. 返回对应索引的服务实例
 * 
 * 算法公式：
 * index = random.nextInt(addresses.size())
 * 
 * 优点：
 * - 实现最简单
 * - 在大量请求下，分布相对均匀
 * - 不需要维护状态，无状态设计
 * 
 * 缺点：
 * - 短时间内可能不均匀
 * - 不考虑服务实例的负载和性能
 * - 随机性可能导致某些实例负载过高
 * 
 * 适用场景：
 * - 服务实例性能相近
 * - 对负载均衡精度要求不高
 * - 需要简单快速的实现
 * 
 * 线程安全：
 * - Random是线程安全的（Java 7+）
 * - 但高并发下可能成为性能瓶颈
 * - 可以考虑使用ThreadLocalRandom
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class RandomLoadBalancer implements LoadBalancer {
    
    /**
     * 随机数生成器
     * Random number generator
     * 
     * 注意：Random是线程安全的，但高并发下性能较差
     * 可以考虑使用ThreadLocalRandom提高性能
     */
    private final Random random = new Random();
    
    /**
     * 选择服务实例 - 随机算法
     * Select service instance randomly
     * 
     * 选择流程：
     * 1. 验证地址列表有效性
     * 2. 如果只有一个实例，直接返回
     * 3. 生成随机索引
     * 4. 返回对应索引的服务实例
     * 
     * @param addresses 服务地址列表
     * @param serviceName 服务名称（本算法不使用，但接口要求）
     * @return 选中的服务地址
     */
    @Override
    public InetSocketAddress select(List<InetSocketAddress> addresses, String serviceName) {
        // 步骤1：验证地址列表
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        
        // 步骤2：单个实例直接返回
        if (addresses.size() == 1) {
            return addresses.get(0);
        }
        
        // 步骤3-4：生成随机索引并返回
        // nextInt(n)：生成[0, n)范围内的随机整数
        int index = random.nextInt(addresses.size());
        return addresses.get(index);
    }
}

