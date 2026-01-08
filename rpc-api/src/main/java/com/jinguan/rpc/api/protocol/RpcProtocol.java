package com.jinguan.rpc.api.protocol;

/**
 * RPC 协议常量定义 - 定义自定义协议的格式和常量
 * RPC Protocol Constants
 * 
 * 协议格式（Protocol format）：
 * +--------+--------+------------+-------------+--------+---------+
 * | Magic  |Version |Serializer  | MessageType |Length  |  Body   |
 * | 4字节  | 1字节  |  1字节     |  1字节      | 4字节  | N字节   |
 * +--------+--------+------------+-------------+--------+---------+
 * 
 * 协议设计说明：
 * 1. Magic Number（魔数）：用于快速识别协议，防止错误数据进入
 * 2. Version（版本号）：支持协议版本升级和兼容性处理
 * 3. Serializer（序列化类型）：支持多种序列化方式，可插拔
 * 4. MessageType（消息类型）：区分请求、响应、心跳等不同消息
 * 5. Length（消息体长度）：解决TCP粘包拆包问题
 * 6. Body（消息体）：实际的业务数据
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class RpcProtocol {
    
    /**
     * 魔数 - 用于协议识别（0xCAFEBABE）
     * Magic number for protocol identification
     * 
     * 类似于Java字节码文件的魔数，用于快速判断是否为有效的RPC消息
     * 如果接收到的数据魔数不匹配，说明不是我们的协议，直接丢弃
     */
    public static final int MAGIC_NUMBER = 0xCAFEBABE;
    
    /**
     * 协议版本号
     * Protocol version
     * 
     * 用于协议升级和版本兼容性管理
     * 当协议格式发生变化时，可以通过版本号来区分不同的协议版本
     */
    public static final byte VERSION = 1;
    
    /**
     * 协议头长度（字节）
     * Header length (bytes)
     * 
     * 固定长度的协议头组成：
     * - Magic Number: 4字节
     * - Version: 1字节
     * - Serializer: 1字节
     * - MessageType: 1字节
     * - Length: 4字节
     * 总计：11字节
     */
    public static final int HEADER_LENGTH = 11;
    
    /**
     * 最大消息帧长度（16MB）
     * Maximum frame length
     * 
     * 限制单个消息的最大大小，防止：
     * 1. 恶意的超大消息攻击
     * 2. 内存溢出
     * 3. 网络拥塞
     */
    public static final int MAX_FRAME_LENGTH = 16 * 1024 * 1024;
    
    /**
     * 序列化算法类型 - 定义支持的序列化方式
     * Serialization algorithms
     * 
     * 支持可插拔的序列化方式，不同场景可选择不同的序列化算法：
     * - JAVA：Java原生序列化，兼容性好但性能较差
     * - JSON：可读性好，跨语言支持，性能中等
     * - PROTOBUF：性能优秀，体积小，适合高性能场景
     * - HESSIAN：二进制序列化，性能好，Dubbo默认使用
     */
    public static class SerializerType {
        /** Java原生序列化 */
        public static final byte JAVA = 0;
        /** JSON序列化（默认） */
        public static final byte JSON = 1;
        /** Protocol Buffers序列化 */
        public static final byte PROTOBUF = 2;
        /** Hessian序列化 */
        public static final byte HESSIAN = 3;
    }
    
    /**
     * 消息类型 - 区分不同用途的消息
     * Message types
     * 
     * 定义消息的类型，用于服务端识别并采取不同的处理策略：
     * - REQUEST：RPC请求消息，需要调用服务方法并返回结果
     * - RESPONSE：RPC响应消息，包含方法调用的结果
     * - HEARTBEAT_REQUEST：心跳请求，用于保持连接活跃
     * - HEARTBEAT_RESPONSE：心跳响应，回复心跳请求
     */
    public static class MessageType {
        /** RPC请求 */
        public static final byte REQUEST = 1;
        /** RPC响应 */
        public static final byte RESPONSE = 2;
        /** 心跳请求 */
        public static final byte HEARTBEAT_REQUEST = 3;
        /** 心跳响应 */
        public static final byte HEARTBEAT_RESPONSE = 4;
    }
}

