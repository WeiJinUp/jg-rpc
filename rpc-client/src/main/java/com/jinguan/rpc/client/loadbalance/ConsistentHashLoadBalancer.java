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
 * Consistent Hash Load Balancer
 * 
 * Uses consistent hashing for stable instance selection
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class ConsistentHashLoadBalancer implements LoadBalancer {
    
    /**
     * Number of virtual nodes for each real node
     */
    private static final int VIRTUAL_NODE_COUNT = 160;
    
    @Override
    public InetSocketAddress select(List<InetSocketAddress> addresses, String serviceName) {
        if (addresses == null || addresses.isEmpty()) {
            return null;
        }
        
        if (addresses.size() == 1) {
            return addresses.get(0);
        }
        
        // Build consistent hash ring
        TreeMap<Long, InetSocketAddress> ring = new TreeMap<>();
        
        for (InetSocketAddress address : addresses) {
            String key = address.toString();
            
            // Add virtual nodes
            for (int i = 0; i < VIRTUAL_NODE_COUNT; i++) {
                long hash = hash(key + "#" + i);
                ring.put(hash, address);
            }
        }
        
        // Hash the service name to find the node
        long hash = hash(serviceName);
        SortedMap<Long, InetSocketAddress> tailMap = ring.tailMap(hash);
        
        // Get the first node (clockwise on the ring)
        Long nodeHash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        return ring.get(nodeHash);
    }
    
    /**
     * Hash function (MD5)
     */
    private long hash(String key) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(key.getBytes(StandardCharsets.UTF_8));
            
            // Use first 8 bytes as long
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (bytes[i] & 0xFF);
            }
            return hash;
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
}

