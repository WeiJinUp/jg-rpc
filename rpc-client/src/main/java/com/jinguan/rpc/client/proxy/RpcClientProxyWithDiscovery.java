package com.jinguan.rpc.client.proxy;

import com.jinguan.rpc.api.dto.RpcRequest;
import com.jinguan.rpc.api.dto.RpcResponse;
import com.jinguan.rpc.client.RpcClientWithDiscovery;
import com.jinguan.rpc.client.netty.NettyRpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;

/**
 * RPC Client Proxy with Service Discovery
 * 
 * Enhanced proxy that supports service discovery, load balancing, and async calls
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class RpcClientProxyWithDiscovery implements InvocationHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcClientProxyWithDiscovery.class);
    
    private final RpcClientWithDiscovery clientManager;
    
    public RpcClientProxyWithDiscovery(RpcClientWithDiscovery clientManager) {
        this.clientManager = clientManager;
    }
    
    /**
     * Create proxy instance for service interface
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
        
        String serviceName = method.getDeclaringClass().getName();
        String methodName = method.getName();
        
        logger.info("Invoking remote method: {}.{}", serviceName, methodName);
        
        // Create RPC request
        RpcRequest request = new RpcRequest();
        request.setInterfaceName(serviceName);
        request.setMethodName(methodName);
        request.setParameters(args);
        request.setParamTypes(method.getParameterTypes());
        
        // Check if method returns CompletableFuture (async call)
        if (CompletableFuture.class.isAssignableFrom(method.getReturnType())) {
            return invokeAsync(serviceName, request);
        }
        
        // Synchronous call
        return invokeSync(serviceName, request);
    }
    
    /**
     * Synchronous RPC call
     */
    private Object invokeSync(String serviceName, RpcRequest request) throws Exception {
        // Get client using service discovery and load balancing
        NettyRpcClient client = clientManager.getClient(serviceName);
        
        // Send request and get response
        RpcResponse response = client.sendRequest(request);
        
        // Handle response
        if (response.isSuccess()) {
            return response.getData();
        } else {
            throw new RuntimeException("RPC call failed: " + response.getErrorMessage());
        }
    }
    
    /**
     * Asynchronous RPC call
     */
    private CompletableFuture<Object> invokeAsync(String serviceName, RpcRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return invokeSync(serviceName, request);
            } catch (Exception e) {
                logger.error("Async RPC call failed", e);
                throw new RuntimeException(e);
            }
        });
    }
}

