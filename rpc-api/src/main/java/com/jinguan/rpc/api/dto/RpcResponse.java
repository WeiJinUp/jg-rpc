package com.jinguan.rpc.api.dto;

import java.io.Serializable;

/**
 * RPC 响应对象 - 封装远程过程调用的结果
 * RPC Response DTO - encapsulates the result of remote procedure call
 * 
 * 该类用于服务端向客户端返回方法调用的结果，包括成功返回的数据或失败的错误信息
 * This class will be serialized and sent back to the client
 *
 * @author JinGuan
 * @version 1.0.0
 */
public class RpcResponse implements Serializable {
    
    /**
     * 序列化版本号，用于确保序列化和反序列化的兼容性
     * Serial version UID for serialization compatibility
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * 方法调用返回的结果数据
     * 当调用成功时，该字段存储方法的返回值
     * The result data returned by the method invocation
     */
    private Object data;
    
    /**
     * 错误信息
     * 当调用失败时，该字段存储错误的详细描述
     * Error message if the invocation failed
     */
    private String errorMessage;
    
    /**
     * 调用成功标志
     * true表示方法调用成功，false表示调用失败
     * Flag indicating whether the invocation was successful
     */
    private boolean success;
    
    /**
     * 请求唯一标识符（第二阶段使用）
     * 用于在异步通信中将响应与对应的请求关联起来
     * Request ID for matching with request (Phase 2)
     */
    private String requestId;
    
    /**
     * 无参构造函数
     * 用于反序列化时创建对象实例
     * Default constructor for deserialization
     */
    public RpcResponse() {
    }
    
    /**
     * 静态工厂方法 - 创建一个成功的响应对象
     * Static factory method - creates a successful response
     * 
     * 使用步骤：
     * 1. 创建一个新的RpcResponse实例
     * 2. 设置返回的数据
     * 3. 设置成功标志为true
     *
     * @param data 方法调用返回的结果数据
     * @return 封装了成功结果的RpcResponse实例
     */
    public static RpcResponse success(Object data) {
        RpcResponse response = new RpcResponse();
        response.setData(data);
        response.setSuccess(true);
        return response;
    }
    
    /**
     * 静态工厂方法 - 创建一个失败的响应对象
     * Static factory method - creates a failed response
     * 
     * 使用步骤：
     * 1. 创建一个新的RpcResponse实例
     * 2. 设置错误信息
     * 3. 设置成功标志为false
     *
     * @param errorMessage 错误信息描述
     * @return 封装了错误信息的RpcResponse实例
     */
    public static RpcResponse fail(String errorMessage) {
        RpcResponse response = new RpcResponse();
        response.setErrorMessage(errorMessage);
        response.setSuccess(false);
        return response;
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    @Override
    public String toString() {
        return "RpcResponse{" +
                "data=" + data +
                ", errorMessage='" + errorMessage + '\'' +
                ", success=" + success +
                '}';
    }
}

