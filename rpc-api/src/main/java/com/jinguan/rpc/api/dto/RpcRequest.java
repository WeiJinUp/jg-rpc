package com.jinguan.rpc.api.dto;

import java.io.Serializable;
import java.util.Arrays;

/**
 * RPC Request DTO - encapsulates remote procedure call information
 * This class will be serialized and sent over the network
 *
 * @author JinGuan
 * @version 1.0.0
 */
public class RpcRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * The fully qualified interface name (e.g., com.jinguan.rpc.api.HelloService)
     */
    private String interfaceName;
    
    /**
     * The method name to invoke (e.g., "hello")
     */
    private String methodName;
    
    /**
     * The actual parameter values
     */
    private Object[] parameters;
    
    /**
     * The parameter types (used for method signature matching)
     */
    private Class<?>[] paramTypes;
    
    public RpcRequest() {
    }
    
    public RpcRequest(String interfaceName, String methodName, Object[] parameters, Class<?>[] paramTypes) {
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.parameters = parameters;
        this.paramTypes = paramTypes;
    }
    
    public String getInterfaceName() {
        return interfaceName;
    }
    
    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
    
    public Object[] getParameters() {
        return parameters;
    }
    
    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }
    
    public Class<?>[] getParamTypes() {
        return paramTypes;
    }
    
    public void setParamTypes(Class<?>[] paramTypes) {
        this.paramTypes = paramTypes;
    }
    
    @Override
    public String toString() {
        return "RpcRequest{" +
                "interfaceName='" + interfaceName + '\'' +
                ", methodName='" + methodName + '\'' +
                ", parameters=" + Arrays.toString(parameters) +
                ", paramTypes=" + Arrays.toString(paramTypes) +
                '}';
    }
}

