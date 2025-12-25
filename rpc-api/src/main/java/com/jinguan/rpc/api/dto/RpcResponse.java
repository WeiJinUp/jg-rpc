package com.jinguan.rpc.api.dto;

import java.io.Serializable;

/**
 * RPC Response DTO - encapsulates the result of remote procedure call
 * This class will be serialized and sent back to the client
 *
 * @author JinGuan
 * @version 1.0.0
 */
public class RpcResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * The result data returned by the method invocation
     */
    private Object data;
    
    /**
     * Error message if the invocation failed
     */
    private String errorMessage;
    
    /**
     * Flag indicating whether the invocation was successful
     */
    private boolean success;
    
    /**
     * Request ID for matching with request (Phase 2)
     */
    private String requestId;
    
    public RpcResponse() {
    }
    
    /**
     * Create a successful response
     *
     * @param data the result data
     * @return RpcResponse instance
     */
    public static RpcResponse success(Object data) {
        RpcResponse response = new RpcResponse();
        response.setData(data);
        response.setSuccess(true);
        return response;
    }
    
    /**
     * Create a failed response
     *
     * @param errorMessage the error message
     * @return RpcResponse instance
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

