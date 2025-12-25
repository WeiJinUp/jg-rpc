package com.jinguan.rpc.client.proxy;

import com.jinguan.rpc.api.dto.RpcRequest;
import com.jinguan.rpc.api.dto.RpcResponse;
import com.jinguan.rpc.client.netty.NettyRpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * RPC Client Dynamic Proxy
 * 
 * Creates proxy instances that make RPC calls transparently
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class RpcClientProxy implements InvocationHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcClientProxy.class);
    
    private final NettyRpcClient client;
    
    public RpcClientProxy(NettyRpcClient client) {
        this.client = client;
    }
    
    /**
     * Create proxy instance for service interface
     * 
     * @param serviceClass service interface class
     * @param <T> service type
     * @return proxy instance
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> serviceClass) {
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class<?>[]{serviceClass},
                this
        );
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Handle Object methods locally
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }
        
        logger.info("Invoking remote method: {}.{}", 
                method.getDeclaringClass().getName(), method.getName());
        
        // Create RPC request
        RpcRequest request = new RpcRequest();
        request.setInterfaceName(method.getDeclaringClass().getName());
        request.setMethodName(method.getName());
        request.setParameters(args);
        request.setParamTypes(method.getParameterTypes());
        
        // Send request and get response
        RpcResponse response = client.sendRequest(request);
        
        // Handle response
        if (response.isSuccess()) {
            return response.getData();
        } else {
            throw new RuntimeException("RPC call failed: " + response.getErrorMessage());
        }
    }
}

