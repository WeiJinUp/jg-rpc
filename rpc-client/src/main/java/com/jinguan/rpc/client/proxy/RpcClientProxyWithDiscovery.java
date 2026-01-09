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
 * 带服务发现的RPC客户端代理 - 第三阶段增强版代理
 * RPC Client Proxy with Service Discovery
 * 
 * 该代理是第二阶段的RpcClientProxy的增强版，新增功能：
 * 1. 服务发现：自动从注册中心发现服务
 * 2. 负载均衡：从多个实例中选择一个
 * 3. 异步调用：支持返回CompletableFuture的异步方法
 * 
 * 与第二阶段代理的区别：
 * - 第二阶段：需要手动指定服务器地址
 * - 第三阶段：自动发现服务，无需知道地址
 * - 第三阶段：支持负载均衡，高可用
 * - 第三阶段：支持异步RPC调用
 * 
 * 工作流程：
 * 1. 客户端调用代理方法
 * 2. invoke()方法被调用
 * 3. 判断方法返回类型（同步/异步）
 * 4. 通过clientManager获取客户端（自动发现+负载均衡）
 * 5. 发送RPC请求
 * 6. 返回结果或Future
 * 
 * 异步调用支持：
 * - 如果方法返回CompletableFuture，使用异步调用
 * - 如果方法返回普通类型，使用同步调用
 * - 自动识别，无需额外配置
 * 
 * @author JinGuan
 * @version 3.0.0
 */
public class RpcClientProxyWithDiscovery implements InvocationHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcClientProxyWithDiscovery.class);
    
    /**
     * 客户端管理器 - 负责服务发现、负载均衡、客户端管理
     */
    private final RpcClientWithDiscovery clientManager;
    
    /**
     * 构造函数
     * 
     * @param clientManager 客户端管理器实例
     */
    public RpcClientProxyWithDiscovery(RpcClientWithDiscovery clientManager) {
        this.clientManager = clientManager;
    }
    
    /**
     * 创建代理实例 - 与第二阶段相同
     * Create proxy instance for service interface
     * 
     * @param serviceClass 服务接口Class
     * @param <T> 服务类型
     * @return 代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> serviceClass) {
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class<?>[]{serviceClass},
                this
        );
    }
    
    /**
     * 代理方法调用 - 核心方法
     * Proxy method invocation
     * 
     * 调用流程（详细步骤）：
     * 1. 处理Object类的方法（toString、equals等）
     * 2. 提取服务名和方法名
     * 3. 创建RpcRequest对象
     * 4. 判断方法返回类型（同步/异步）
     * 5. 调用对应的处理方法
     * 
     * 同步vs异步判断：
     * - 如果返回类型是CompletableFuture或其子类，使用异步调用
     * - 否则使用同步调用
     * 
     * @param proxy 代理对象
     * @param method 被调用的方法
     * @param args 方法参数
     * @return 方法返回值
     * @throws Throwable 可能抛出的异常
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 步骤1：处理Object类的方法
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }
        
        // 步骤2：提取服务信息
        String serviceName = method.getDeclaringClass().getName();
        String methodName = method.getName();
        
        logger.info("Invoking remote method: {}.{}", serviceName, methodName);
        
        // 步骤3：创建RPC请求
        RpcRequest request = new RpcRequest();
        request.setInterfaceName(serviceName);
        request.setMethodName(methodName);
        request.setParameters(args);
        request.setParamTypes(method.getParameterTypes());
        
        // 步骤4-5：判断返回类型并调用
        // 如果方法返回CompletableFuture，使用异步调用
        if (CompletableFuture.class.isAssignableFrom(method.getReturnType())) {
            return invokeAsync(serviceName, request);
        }
        
        // 否则使用同步调用
        return invokeSync(serviceName, request);
    }
    
    /**
     * 同步RPC调用
     * Synchronous RPC call
     * 
     * 调用流程：
     * 1. 通过clientManager获取客户端（自动发现+负载均衡）
     * 2. 发送请求并等待响应
     * 3. 处理响应结果
     * 
     * 与第二阶段的区别：
     * - 第二阶段：直接使用固定的客户端
     * - 第三阶段：每次调用都可能选择不同的服务实例
     * 
     * @param serviceName 服务名称
     * @param request RPC请求
     * @return 方法返回值
     * @throws Exception 如果调用失败
     */
    private Object invokeSync(String serviceName, RpcRequest request) throws Exception {
        // 步骤1：获取客户端（自动发现+负载均衡）
        // 这里会从注册中心发现服务，并使用负载均衡器选择实例
        NettyRpcClient client = clientManager.getClient(serviceName);
        
        // 步骤2：发送请求并等待响应（阻塞）
        RpcResponse response = client.sendRequest(request);
        
        // 步骤3：处理响应
        if (response.isSuccess()) {
            return response.getData();
        } else {
            throw new RuntimeException("RPC call failed: " + response.getErrorMessage());
        }
    }
    
    /**
     * 异步RPC调用
     * Asynchronous RPC call
     * 
     * 调用流程：
     * 1. 使用CompletableFuture.supplyAsync()异步执行
     * 2. 在后台线程中调用同步方法
     * 3. 立即返回Future，不阻塞调用线程
     * 
     * 为什么这样实现？
     * - 复用同步调用的逻辑
     * - 在后台线程中执行，不阻塞
     * - 客户端可以继续执行其他操作
     * 
     * 使用场景：
     * - 服务端方法返回CompletableFuture
     * - 客户端需要并发处理多个请求
     * - 不阻塞主线程的场景
     * 
     * @param serviceName 服务名称
     * @param request RPC请求
     * @return CompletableFuture，包含方法返回值
     */
    private CompletableFuture<Object> invokeAsync(String serviceName, RpcRequest request) {
        // 使用supplyAsync在后台线程执行同步调用
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 在后台线程中执行同步调用
                return invokeSync(serviceName, request);
            } catch (Exception e) {
                logger.error("Async RPC call failed", e);
                throw new RuntimeException(e);
            }
        });
    }
}

