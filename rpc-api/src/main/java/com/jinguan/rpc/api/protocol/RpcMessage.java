package com.jinguan.rpc.api.protocol;

/**
 * RPC 消息对象 - 封装请求/响应及协议元数据
 * RPC Message - wraps request/response with protocol metadata
 * 
 * 该类是对RpcRequest/RpcResponse的进一步封装，添加了协议层的元数据信息
 * 
 * 设计目的：
 * 1. 分离业务层（RpcRequest/Response）和协议层（RpcMessage）
 * 2. 支持多种消息类型（请求、响应、心跳等）
 * 3. 支持可插拔的序列化方式
 * 4. 便于协议扩展和版本升级
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class RpcMessage {
    
    /**
     * 序列化算法类型
     * Serialization algorithm type
     * 
     * 指定消息体使用的序列化方式（Java/JSON/Protobuf/Hessian）
     * 不同的消息可以使用不同的序列化方式，提供灵活性
     */
    private byte serializerType;
    
    /**
     * 消息类型
     * Message type (request/response/heartbeat)
     * 
     * 标识消息的用途：
     * - REQUEST: RPC请求消息
     * - RESPONSE: RPC响应消息
     * - HEARTBEAT_REQUEST/RESPONSE: 心跳消息
     * 
     * 服务端根据消息类型进行不同的处理
     */
    private byte messageType;
    
    /**
     * 消息体 - 实际的业务数据
     * Message body (RpcRequest or RpcResponse)
     * 
     * 根据messageType的不同，data的类型也不同：
     * - REQUEST: data为RpcRequest对象
     * - RESPONSE: data为RpcResponse对象
     * - HEARTBEAT: data为简单的字符串（如"ping"/"pong"）
     */
    private Object data;
    
    /**
     * 请求唯一标识符
     * Request ID for matching request and response
     * 
     * 用于在异步通信中匹配请求和响应：
     * 1. 客户端发送请求时生成requestId
     * 2. 服务端返回响应时携带相同的requestId
     * 3. 客户端根据requestId找到对应的请求并完成Future
     * 
     * 这是实现异步RPC的关键
     */
    private String requestId;
    
    /**
     * 无参构造函数
     * Default constructor
     */
    public RpcMessage() {
    }
    
    /**
     * 全参构造函数
     * Full constructor
     * 
     * @param serializerType 序列化类型
     * @param messageType 消息类型
     * @param data 消息体数据
     * @param requestId 请求ID
     */
    public RpcMessage(byte serializerType, byte messageType, Object data, String requestId) {
        this.serializerType = serializerType;
        this.messageType = messageType;
        this.data = data;
        this.requestId = requestId;
    }
    
    public byte getSerializerType() {
        return serializerType;
    }
    
    public void setSerializerType(byte serializerType) {
        this.serializerType = serializerType;
    }
    
    public byte getMessageType() {
        return messageType;
    }
    
    public void setMessageType(byte messageType) {
        this.messageType = messageType;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    @Override
    public String toString() {
        return "RpcMessage{" +
                "serializerType=" + serializerType +
                ", messageType=" + messageType +
                ", requestId='" + requestId + '\'' +
                ", data=" + data +
                '}';
    }
}

