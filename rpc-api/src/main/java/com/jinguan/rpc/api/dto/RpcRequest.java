package com.jinguan.rpc.api.dto;

import java.io.Serializable;
import java.util.Arrays;

/**
 * RPC 请求对象 - 封装远程过程调用的信息
 * RPC Request DTO - encapsulates remote procedure call information
 * 
 * 该类用于在客户端和服务端之间传输RPC调用信息，包括要调用的接口、方法、参数等
 * This class will be serialized and sent over the network
 *
 * @author JinGuan
 * @version 1.0.0
 */
public class RpcRequest implements Serializable {
    
    /**
     * 序列化版本号，用于确保序列化和反序列化的兼容性
     * Serial version UID for serialization compatibility
     */
    private static final long serialVersionUID = 1L;
    
    /**
     * 接口全限定名（例如：com.jinguan.rpc.api.HelloService）
     * 用于在服务端定位具体的服务实现类
     * The fully qualified interface name (e.g., com.jinguan.rpc.api.HelloService)
     */
    private String interfaceName;
    
    /**
     * 要调用的方法名（例如："hello"）
     * 用于通过反射机制找到具体要执行的方法
     * The method name to invoke (e.g., "hello")
     */
    private String methodName;
    
    /**
     * 方法参数的实际值
     * 按照方法签名的参数顺序传递
     * The actual parameter values
     */
    private Object[] parameters;
    
    /**
     * 方法参数的类型数组
     * 用于方法签名匹配，解决方法重载的问题
     * The parameter types (used for method signature matching)
     */
    private Class<?>[] paramTypes;
    
    /**
     * 请求唯一标识符（第二阶段使用）
     * 用于在异步通信中匹配请求和响应
     * Request ID for matching request and response (Phase 2)
     */
    private String requestId;
    
    /**
     * 无参构造函数
     * 用于反序列化时创建对象实例
     * Default constructor for deserialization
     */
    public RpcRequest() {
    }
    
    /**
     * 全参构造函数 - 创建一个完整的RPC请求对象
     * Full constructor - creates a complete RPC request object
     *
     * @param interfaceName 接口全限定名
     * @param methodName 方法名
     * @param parameters 方法参数值数组
     * @param paramTypes 方法参数类型数组
     */
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
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
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

