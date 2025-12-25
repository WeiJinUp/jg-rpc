package com.jinguan.rpc.api.protocol;

/**
 * RPC Message - wraps request/response with protocol metadata
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class RpcMessage {
    
    /**
     * Serialization algorithm type
     */
    private byte serializerType;
    
    /**
     * Message type (request/response/heartbeat)
     */
    private byte messageType;
    
    /**
     * Message body (RpcRequest or RpcResponse)
     */
    private Object data;
    
    /**
     * Request ID for matching request and response
     */
    private String requestId;
    
    public RpcMessage() {
    }
    
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

