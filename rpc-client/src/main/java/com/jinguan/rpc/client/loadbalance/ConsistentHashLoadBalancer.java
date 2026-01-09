package com.jinguan.rpc.client.loadbalance;

import com.jinguan.rpc.api.loadbalance.LoadBalancer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 一致性哈希负载均衡器 - 基于一致性哈希算法
 * Consistent Hash Load Balancer
 * 
 * 该负载均衡器使用一致性哈希算法，保证相同服务名总是选择相同的服务实例
 * 
 * 一致性哈希原理：
 * 1. 将服务实例映射到哈希环上
 * 2. 将服务名也映射到哈希环上
 * 3. 顺时针找到第一个服务实例节点
 * 
 * 哈希环示意图：
 * <pre>
 *            Hash Ring
 *               0
 *               |
 *        A1     |     A2
 *         \     |     /
 *          \    |    /
 *           \   |   /
 *            \  |  /
 *             \ | /
 *              \|/
 *            Service
 * </pre>
 * 
 * 虚拟节点（Virtual Nodes）：
 * - 每个真实节点创建多个虚拟节点（默认160个）
 * - 虚拟节点分布在哈希环的不同位置
 * - 提高负载分布的均匀性
 * - 减少节点增减时的数据迁移
 * 
 * 优点：
 * - 相同服务名总是选择相同实例（会话粘性）
 * - 节点增减时，只影响相邻节点
 * - 负载分布相对均匀
 * 
 * 缺点：
 * - 实现复杂
 * - 需要维护哈希环
 * - 虚拟节点数量需要合理设置
 * 
 * 适用场景：
 * - 需要会话粘性的场景
 * - 服务实例经常变化的场景
 * - 需要减少数据迁移的场景
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class ConsistentHashLoadBalancer implements LoadBalancer {
    
    /**
     * 虚拟节点数量 - 每个真实节点对应的虚拟节点数
     * Number of virtual nodes for each real node
     * 
     * 为什么是160？
     * - 经验值，平衡性能和均匀性
     * - 太少：负载分布不均匀
     * - 太多：计算开销大
     * - 160是Dubbo等框架的常用值
     */
    private static final int VIRTUAL_NODE_COUNT = 160;
    
    /**
     * 选择服务实例 - 一致性哈希算法
     * Select service instance using consistent hashing
     * 
     * 选择流程（详细步骤）：
     * 1. 验证地址列表有效性
     * 2. 构建一致性哈希环（TreeMap）
     * 3. 为每个服务实例创建虚拟节点
     * 4. 将服务名哈希，在环上定位
     * 5. 顺时针找到第一个节点
     * 6. 返回对应的服务实例
     * 
     * 为什么使用TreeMap？
     * - TreeMap是有序的，便于查找
     * - tailMap()可以快速找到大于等于指定值的所有节点
     * - 时间复杂度：O(log n)
     * 
     * 为什么使用服务名而不是请求参数？
     * - 保证相同服务总是选择相同实例
     * - 如果需要基于请求参数，可以传入requestId等
     * 
     * @param addresses 服务地址列表
     * @param serviceName 服务名称（用于哈希定位）
     * @return 选中的服务地址
     */
    @Override
    public InetSocketAddress select(List<InetSocketAddress> addresses, String serviceName) {
        // 步骤1：验证地址列表
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        
        if (addresses.size() == 1) {
            return addresses.get(0);
        }
        
        // 步骤2：构建一致性哈希环
        // TreeMap<哈希值, 服务地址>
        TreeMap<Long, InetSocketAddress> ring = new TreeMap<>();
        
        // 步骤3：为每个服务实例创建虚拟节点
        for (InetSocketAddress address : addresses) {
            String key = address.toString();
            
            // 为每个真实节点创建VIRTUAL_NODE_COUNT个虚拟节点
            for (int i = 0; i < VIRTUAL_NODE_COUNT; i++) {
                // 虚拟节点key：真实地址 + "#" + 序号
                long hash = hash(key + "#" + i);
                ring.put(hash, address);
            }
        }
        
        // 步骤4：将服务名哈希，在环上定位
        long hash = hash(serviceName);
        
        // 步骤5：顺时针找到第一个节点
        // tailMap(hash)：返回所有大于等于hash的节点
        SortedMap<Long, InetSocketAddress> tailMap = ring.tailMap(hash);
        
        // 如果tailMap为空，说明服务名哈希值超过了环的最大值
        // 此时选择环上的第一个节点（环是循环的）
        Long nodeHash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        
        // 步骤6：返回对应的服务实例
        return ring.get(nodeHash);
    }
    
    /**
     * 哈希函数 - 使用MD5算法
     * Hash function (MD5)
     * 
     * 哈希流程：
     * 1. 使用MD5算法计算字符串的哈希值
     * 2. 取MD5结果的前8个字节
     * 3. 转换为long类型的哈希值
     * 
     * 为什么使用MD5？
     * - 分布均匀，减少哈希冲突
     * - 计算速度较快
     * - 结果稳定（相同输入总是相同输出）
     * 
     * 为什么只取前8字节？
     * - long类型占8字节
     * - 足够表示哈希环上的位置
     * - 减少计算开销
     * 
     * 哈希值范围：
     * - long范围：-2^63 到 2^63-1
     * - 实际使用无符号解释：0 到 2^64-1
     * 
     * @param key 要哈希的字符串（服务地址或服务名）
     * @return 64位哈希值
     */
    private long hash(String key) {
        try {
            // 步骤1：使用MD5算法
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(key.getBytes(StandardCharsets.UTF_8));
            
            // 步骤2-3：取前8字节转换为long
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                // 左移8位，然后或运算
                hash = (hash << 8) | (bytes[i] & 0xFF);
            }
            return hash;
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}

