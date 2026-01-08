package com.jinguan.rpc.api.serializer;

import com.jinguan.rpc.api.protocol.RpcProtocol;

import java.util.HashMap;
import java.util.Map;

/**
 * 序列化器工厂 - 管理序列化器实例
 * Serializer Factory - manages serializer instances
 * 
 * 该工厂类负责序列化器的注册和获取，实现可插拔的序列化机制
 * 
 * 设计模式：
 * 1. 工厂模式：统一管理序列化器实例的创建和获取
 * 2. 单例模式：所有序列化器都是单例，通过类加载器保证线程安全
 * 3. 策略模式：不同的序列化算法实现统一的Serializer接口
 * 
 * 可插拔设计：
 * - 用户可以通过register方法注册自定义的序列化器
 * - 支持运行时动态添加新的序列化算法
 * - 通过type字节标识不同的序列化器
 * 
 * 使用场景：
 * - 编码器：根据RpcMessage的serializerType获取序列化器进行序列化
 * - 解码器：根据协议头的serializerType获取序列化器进行反序列化
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class SerializerFactory {
    
    /**
     * 序列化器注册表
     * Key: 序列化类型字节（如：0=Java, 1=JSON）
     * Value: 序列化器实例
     * 
     * 使用static保证全局唯一，所有线程共享
     */
    private static final Map<Byte, Serializer> SERIALIZER_MAP = new HashMap<>();
    
    /**
     * 静态代码块 - 注册默认的序列化器
     * Static block - registers default serializers
     * 
     * 在类加载时执行，自动注册内置的序列化器：
     * - JavaSerializer: Java原生序列化，兼容性好
     * - JsonSerializer: JSON序列化，可读性好，默认使用
     * 
     * 用户可以通过register方法注册更多序列化器：
     * - ProtobufSerializer: Google Protocol Buffers
     * - HessianSerializer: Hessian二进制序列化
     * - KryoSerializer: Kryo高性能序列化
     * 等等...
     */
    static {
        // 注册Java原生序列化器
        register(new JavaSerializer());
        // 注册JSON序列化器（推荐使用）
        register(new JsonSerializer());
    }
    
    /**
     * 注册序列化器 - 添加新的序列化器到工厂
     * Register a serializer
     * 
     * 注册步骤：
     * 1. 从序列化器获取其类型标识（getType()）
     * 2. 将<类型, 实例>映射关系存入注册表
     * 3. 如果类型已存在，会覆盖旧的序列化器
     * 
     * 使用示例：
     * <pre>
     * // 注册自定义序列化器
     * SerializerFactory.register(new MyCustomSerializer());
     * </pre>
     * 
     * @param serializer 序列化器实例
     */
    public static void register(Serializer serializer) {
        SERIALIZER_MAP.put(serializer.getType(), serializer);
    }
    
    /**
     * 根据类型获取序列化器
     * Get serializer by type
     * 
     * 获取流程：
     * 1. 从注册表中查找指定类型的序列化器
     * 2. 如果找到则返回序列化器实例
     * 3. 如果未找到则抛出IllegalArgumentException异常
     * 
     * 使用场景：
     * - 编码器：SerializerFactory.getSerializer(msg.getSerializerType())
     * - 解码器：SerializerFactory.getSerializer(serializerType)
     * 
     * @param type 序列化类型（如：RpcProtocol.SerializerType.JSON）
     * @return 对应的序列化器实例
     * @throws IllegalArgumentException 如果类型不支持
     */
    public static Serializer getSerializer(byte type) {
        Serializer serializer = SERIALIZER_MAP.get(type);
        if (serializer == null) {
            throw new IllegalArgumentException("Unsupported serializer type: " + type);
        }
        return serializer;
    }
    
    /**
     * 获取默认序列化器（JSON）
     * Get default serializer (JSON)
     * 
     * 默认使用JSON序列化器的原因：
     * 1. 可读性好：便于调试和日志查看
     * 2. 跨语言：支持多语言客户端
     * 3. 体积适中：比Java序列化小，比Protobuf稍大
     * 4. 易用性高：无需定义IDL文件
     * 
     * 生产环境建议：
     * - 如果追求性能，可使用Protobuf或Hessian
     * - 如果需要调试，使用JSON
     * - 如果兼容老系统，使用Java序列化
     * 
     * @return JSON序列化器实例
     */
    public static Serializer getDefaultSerializer() {
        return getSerializer(RpcProtocol.SerializerType.JSON);
    }
}

