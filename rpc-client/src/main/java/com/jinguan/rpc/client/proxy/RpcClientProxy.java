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
 * RPC 客户端动态代理 - 实现透明的RPC调用
 * RPC Client Dynamic Proxy
 * 
 * 该类使用JDK动态代理实现透明的RPC调用，让客户端像调用本地方法一样调用远程服务
 * 
 * 核心特性：
 * 1. 透明调用：客户端无需手动创建RpcRequest，像调用本地方法一样调用
 * 2. 动态代理：运行时动态生成代理类，无需为每个服务接口编写代理实现
 * 3. 统一处理：所有RPC调用逻辑集中在invoke方法中，便于维护和扩展
 * 4. 类型安全：泛型保证代理对象的类型安全
 * 
 * 使用示例：
 * <pre>
 * // 创建代理工厂
 * RpcClientProxy proxyFactory = new RpcClientProxy(client);
 * 
 * // 获取代理对象 - 看起来像本地对象！
 * HelloService helloService = proxyFactory.getProxy(HelloService.class);
 * 
 * // 调用方法 - 实际上会发起RPC调用
 * String result = helloService.hello("World");
 * </pre>
 * 
 * 工作原理：
 * 1. 客户端调用代理对象的方法
 * 2. JVM拦截调用，转发到invoke方法
 * 3. invoke方法提取方法信息，创建RpcRequest
 * 4. 通过NettyRpcClient发送请求
 * 5. 接收响应并返回结果
 * 
 * 相比手动创建RpcRequest的优势：
 * - 代码更简洁：一行代码完成RPC调用
 * - 类型安全：编译期检查方法签名
 * - 易于测试：可以用Mock对象替换代理
 * - 面向接口编程：客户端只依赖接口，不依赖实现
 * 
 * 设计模式：
 * - 代理模式：代理对象控制对远程服务的访问
 * - 装饰器模式：在方法调用前后添加RPC通信逻辑
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class RpcClientProxy implements InvocationHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcClientProxy.class);
    
    /**
     * Netty RPC客户端
     * 代理对象通过该客户端发送RPC请求
     */
    private final NettyRpcClient client;
    
    /**
     * 构造函数
     * Constructor
     * 
     * @param client Netty RPC客户端实例
     */
    public RpcClientProxy(NettyRpcClient client) {
        this.client = client;
    }
    
    /**
     * 创建服务接口的代理实例
     * Create proxy instance for service interface
     * 
     * 该方法使用JDK动态代理创建代理对象
     * 
     * 动态代理原理：
     * 1. JVM在运行时动态生成代理类（如：$Proxy0）
     * 2. 代理类实现指定的接口（serviceClass）
     * 3. 代理类的所有方法调用都会转发到InvocationHandler.invoke()
     * 4. 我们在invoke方法中实现RPC调用逻辑
     * 
     * Proxy.newProxyInstance参数说明：
     * @param ClassLoader 类加载器，用于加载代理类
     * @param Class<?>[] 要实现的接口数组
     * @param InvocationHandler 调用处理器，处理方法调用
     * 
     * 为什么要用动态代理？
     * - 避免为每个服务接口手写代理类
     * - 统一的RPC调用逻辑
     * - 支持运行时动态添加新服务
     * 
     * @param serviceClass 服务接口的Class对象
     * @param <T> 服务接口类型
     * @return 实现了该接口的代理对象
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> serviceClass) {
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),  // 使用接口的类加载器
                new Class<?>[]{serviceClass},   // 代理类要实现的接口
                this                            // 调用处理器（this）
        );
    }
    
    /**
     * 代理方法调用 - InvocationHandler的核心方法
     * Proxy method invocation - core method of InvocationHandler
     * 
     * 当客户端调用代理对象的任何方法时，该方法都会被调用
     * 
     * 工作流程（6步）：
     * 1. 判断是否为Object类的方法（toString、equals、hashCode）
     * 2. 记录方法调用日志
     * 3. 根据Method对象提取方法信息，创建RpcRequest
     * 4. 通过NettyRpcClient发送请求
     * 5. 接收RpcResponse响应
     * 6. 根据响应结果返回数据或抛出异常
     * 
     * 参数说明：
     * @param proxy 代理对象本身（通常不使用）
     * @param method 被调用的方法对象，包含方法名、参数类型等信息
     * @param args 方法参数值数组
     * @return 方法的返回值
     * @throws Throwable 可能抛出的任何异常
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 步骤1：处理Object类的方法
        // 对于toString、equals、hashCode等方法，在本地执行，不发起RPC调用
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }
        
        // 步骤2：记录方法调用日志
        logger.info("Invoking remote method: {}.{}", 
                method.getDeclaringClass().getName(), method.getName());
        
        // 步骤3：创建RPC请求对象
        // 从Method对象中提取方法信息
        RpcRequest request = new RpcRequest();
        request.setInterfaceName(method.getDeclaringClass().getName());  // 接口全限定名
        request.setMethodName(method.getName());                          // 方法名
        request.setParameters(args);                                       // 参数值
        request.setParamTypes(method.getParameterTypes());                // 参数类型
        
        // 步骤4：发送请求并获取响应
        // 调用NettyRpcClient的sendRequest方法
        // 该方法会阻塞等待响应（虽然底层是异步的）
        RpcResponse response = client.sendRequest(request);
        
        // 步骤5和6：处理响应
        if (response.isSuccess()) {
            // 调用成功，返回结果
            return response.getData();
        } else {
            // 调用失败，抛出异常
            // 客户端会捕获到这个异常
            throw new RuntimeException("RPC call failed: " + response.getErrorMessage());
        }
    }
}

