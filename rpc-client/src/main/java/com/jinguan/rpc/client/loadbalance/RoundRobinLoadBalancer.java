package com.jinguan.rpc.client.loadbalance;

import com.jinguan.rpc.api.loadbalance.LoadBalancer;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询负载均衡器 - 按顺序依次选择服务实例
 * Round Robin Load Balancer
 * 
 * 该负载均衡器按照轮询的方式，依次选择服务实例
 * 
 * 工作原理：
 * 1. 为每个服务维护一个计数器
 * 2. 每次选择时，计数器自增
 * 3. 使用计数器对服务数量取模，得到索引
 * 4. 返回对应索引的服务实例
 * 
 * 示例：
 * 假设有3个服务实例：[A, B, C]
 * 第1次调用：counter=0, index=0%3=0, 选择A
 * 第2次调用：counter=1, index=1%3=1, 选择B
 * 第3次调用：counter=2, index=2%3=2, 选择C
 * 第4次调用：counter=3, index=3%3=0, 选择A（循环）
 * 
 * 优点：
 * - 实现简单，性能好
 * - 请求分布均匀
 * - 适合所有服务实例性能相近的场景
 * 
 * 缺点：
 * - 不考虑服务实例的负载情况
 * - 不考虑服务实例的性能差异
 * - 服务实例数量变化时，可能造成短时间不均匀
 * 
 * 线程安全：
 * - 使用ConcurrentHashMap保证线程安全
 * - 使用AtomicInteger保证计数器的原子性
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class RoundRobinLoadBalancer implements LoadBalancer {
    
    /**
     * 服务计数器Map - 为每个服务维护独立的计数器
     * Counter for each service
     * 
     * Key: 服务名称
     * Value: 原子计数器，用于轮询选择
     * 
     * 为什么需要为每个服务维护独立的计数器？
     * - 不同服务的实例数量可能不同
     * - 保证每个服务的轮询是独立的
     * - 避免服务间的相互影响
     */
    private final ConcurrentHashMap<String, AtomicInteger> counterMap = new ConcurrentHashMap<>();
    
    /**
     * 选择服务实例 - 轮询算法
     * Select service instance using round robin
     * 
     * 选择流程：
     * 1. 验证地址列表有效性
     * 2. 如果只有一个实例，直接返回
     * 3. 获取或创建该服务的计数器
     * 4. 计数器自增并取模，得到索引
     * 5. 返回对应索引的服务实例
     * 
     * 算法公式：
     * index = (counter++) % addresses.size()
     * 
     * 为什么使用AtomicInteger？
     * - 保证多线程环境下的原子性
     * - getAndIncrement()是原子操作
     * - 避免并发选择时出现重复或遗漏
     * 
     * @param addresses 服务地址列表
     * @param serviceName 服务名称（用于区分不同服务的计数器）
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
        
        // 步骤3：获取或创建计数器
        // computeIfAbsent：如果不存在则创建，保证线程安全
        AtomicInteger counter = counterMap.computeIfAbsent(serviceName, k -> new AtomicInteger(0));
        
        // 步骤4-5：计算索引并返回
        // getAndIncrement()：先获取当前值，再自增（原子操作）
        // 取模运算：确保索引在有效范围内
        int index = counter.getAndIncrement() % addresses.size();
        
        return addresses.get(index);
    }
}

