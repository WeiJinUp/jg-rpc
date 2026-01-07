package com.jinguan.rpc.server;

import com.jinguan.rpc.api.dto.RpcRequest;
import com.jinguan.rpc.api.dto.RpcResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RPC 服务器 - 处理服务注册和请求处理
 * RPC Server - handles service registration and request processing
 * 
 * 这是RPC框架的核心服务端组件，主要功能包括：
 * 1. 服务注册：将服务实现类注册到服务注册表
 * 2. 网络监听：使用ServerSocket监听客户端连接
 * 3. 请求处理：接收RPC请求，通过反射调用实际方法，返回结果
 * 4. 并发处理：使用线程池处理多个客户端的并发请求
 * 
 * Uses native Java Socket and reflection for RPC implementation
 *
 * @author JinGuan
 * @version 1.0.0
 */
public class RpcServer {
    
    /**
     * 默认服务器端口号
     * Default server port
     */
    private static final int DEFAULT_PORT = 8888;
    
    /**
     * 服务注册表 - 存储接口名称到服务实现实例的映射
     * Key: 接口全限定名（如：com.jinguan.rpc.api.HelloService）
     * Value: 该接口的实现类实例
     * Service registry - maps interface name to service implementation instance
     */
    private final Map<String, Object> serviceMap = new HashMap<>();
    
    /**
     * 线程池 - 用于处理客户端请求
     * 采用固定大小的线程池，避免线程无限创建导致资源耗尽
     * Thread pool for handling client requests
     */
    private final ExecutorService executorService;
    
    /**
     * 服务器监听端口
     * Server port
     */
    private final int port;
    
    /**
     * 服务器运行状态标志
     * volatile确保多线程环境下的可见性
     * Server running flag
     */
    private volatile boolean running = false;
    
    /**
     * 无参构造函数 - 使用默认端口8888
     * Default constructor - uses default port 8888
     */
    public RpcServer() {
        this(DEFAULT_PORT);
    }
    
    /**
     * 有参构造函数 - 指定服务器监听端口
     * Constructor with port - specifies the server listening port
     * 
     * 构造函数执行步骤：
     * 1. 设置服务器监听端口
     * 2. 创建固定大小的线程池，大小为CPU核心数的2倍
     *    - 这是一个经验值，可以根据实际业务调整
     *    - 对于I/O密集型任务，线程数可以设置得更大
     * 
     * @param port 服务器监听端口号
     */
    public RpcServer(int port) {
        this.port = port;
        // 创建固定大小的线程池，线程数量 = CPU核心数 * 2
        // 使用固定线程池可以：
        // 1. 避免频繁创建和销毁线程的开销
        // 2. 限制并发线程数量，防止系统资源耗尽
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    }
    
    /**
     * 注册服务实现 - 将服务实现类注册到服务注册表
     * Register a service implementation
     * 
     * 该方法会自动提取服务实现类的第一个接口作为服务名称
     * 客户端通过该接口名称来调用服务
     * The first interface of the service implementation will be used as the service name
     * 
     * 注册流程：
     * 1. 验证服务实例不为空
     * 2. 获取服务实现类的所有接口
     * 3. 验证至少实现了一个接口
     * 4. 取第一个接口的全限定名作为服务标识
     * 5. 将接口名和服务实例存入注册表
     * 6. 打印注册成功日志
     *
     * @param service 服务实现类实例
     * @throws IllegalArgumentException 如果服务为null或未实现任何接口
     */
    public void register(Object service) {
        // 步骤1：验证服务实例不为空
        if (service == null) {
            throw new IllegalArgumentException("Service cannot be null");
        }
        
        // 步骤2：获取服务实现类的所有接口
        Class<?>[] interfaces = service.getClass().getInterfaces();
        
        // 步骤3：验证至少实现了一个接口
        // RPC框架要求服务必须通过接口定义，以实现客户端和服务端的解耦
        if (interfaces.length == 0) {
            throw new IllegalArgumentException("Service must implement at least one interface");
        }
        
        // 步骤4和5：取第一个接口的全限定名，并注册到服务Map中
        String interfaceName = interfaces[0].getName();
        serviceMap.put(interfaceName, service);
        
        // 步骤6：打印注册成功日志
        System.out.println("[RpcServer] Registered service: " + interfaceName);
    }
    
    /**
     * 启动RPC服务器 - 开始监听客户端连接
     * Start the RPC server
     * 
     * 该方法是服务器的核心运行方法，执行以下操作：
     * 1. 检查服务器是否已经在运行
     * 2. 创建ServerSocket并绑定到指定端口
     * 3. 进入无限循环，持续监听客户端连接
     * 4. 每当有客户端连接时，将请求提交给线程池异步处理
     * 
     * 注意：这是一个阻塞方法，会一直运行直到服务器关闭或发生异常
     */
    public void start() {
        // 步骤1：检查服务器是否已经在运行
        // 避免重复启动导致端口冲突
        if (running) {
            System.out.println("[RpcServer] Server is already running on port " + port);
            return;
        }
        
        // 设置运行标志为true
        running = true;
        System.out.println("[RpcServer] Starting RPC server on port " + port + "...");
        
        // 步骤2：使用try-with-resources自动管理ServerSocket资源
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[RpcServer] RPC server started successfully, listening on port " + port);
            
            // 步骤3：进入主循环，持续监听客户端连接
            while (running) {
                // 阻塞等待客户端连接
                // accept()方法会一直阻塞，直到有客户端连接
                Socket clientSocket = serverSocket.accept();
                System.out.println("[RpcServer] Accepted connection from " + clientSocket.getRemoteSocketAddress());
                
                // 步骤4：将客户端请求提交给线程池处理
                // 使用Lambda表达式创建任务
                // 这样可以快速释放主线程，继续接收新的连接
                executorService.execute(() -> handleRequest(clientSocket));
            }
        } catch (IOException e) {
            // 捕获IO异常，如端口已被占用、网络错误等
            System.err.println("[RpcServer] Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 无论正常退出还是异常退出，都要释放资源
            shutdown();
        }
    }
    
    /**
     * 处理客户端请求 - 读取请求、处理请求、返回响应
     * Handle client request
     * 
     * 该方法在线程池中被调用，处理单个客户端的RPC请求
     * 处理流程：
     * 1. 从Socket输入流读取并反序列化RpcRequest对象
     * 2. 调用processRequest方法处理请求（通过反射调用实际方法）
     * 3. 将RpcResponse对象序列化并写入Socket输出流
     * 4. 关闭客户端Socket连接
     *
     * @param clientSocket 客户端Socket连接
     */
    private void handleRequest(Socket clientSocket) {
        // 使用try-with-resources自动管理输入输出流
        // 注意：ObjectOutputStream必须先于ObjectInputStream创建
        // 否则可能导致阻塞
        try (ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream())) {
            
            // 步骤1：从输入流读取并反序列化RPC请求对象
            // readObject()会将字节流反序列化为Java对象
            RpcRequest request = (RpcRequest) input.readObject();
            System.out.println("[RpcServer] Received request: " + request);
            
            // 步骤2：处理请求并获取响应
            // 该方法会通过反射调用实际的服务方法
            RpcResponse response = processRequest(request);
            
            // 步骤3：将响应对象序列化并写入输出流
            // writeObject()会将Java对象序列化为字节流
            output.writeObject(response);
            // flush()确保所有数据都发送到客户端
            output.flush();
            System.out.println("[RpcServer] Sent response: " + response);
            
        } catch (IOException | ClassNotFoundException e) {
            // 捕获IO异常和类找不到异常
            System.err.println("[RpcServer] Error handling request: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 步骤4：无论是否发生异常，都要关闭客户端Socket
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("[RpcServer] Error closing client socket: " + e.getMessage());
            }
        }
    }
    
    /**
     * 处理RPC请求 - 使用反射机制调用实际的服务方法
     * Process RPC request using reflection
     * 
     * 这是RPC框架的核心方法，通过反射实现动态方法调用
     * 处理流程：
     * 1. 根据接口名从服务注册表获取服务实现实例
     * 2. 根据方法名和参数类型获取Method对象
     * 3. 使用Method.invoke()调用实际方法
     * 4. 将返回结果封装成RpcResponse对象
     * 
     * 反射的优势：
     * - 客户端和服务端无需耦合具体实现
     * - 可以在运行时动态调用任何已注册的服务方法
     * - 实现了真正的远程过程调用抽象
     *
     * @param request RPC请求对象，包含接口名、方法名、参数等信息
     * @return RPC响应对象，包含方法执行结果或错误信息
     */
    private RpcResponse processRequest(RpcRequest request) {
        try {
            // 步骤1：从服务注册表获取服务实现实例
            String interfaceName = request.getInterfaceName();
            Object serviceImpl = serviceMap.get(interfaceName);
            
            // 验证服务是否已注册
            if (serviceImpl == null) {
                return RpcResponse.fail("Service not found: " + interfaceName);
            }
            
            // 步骤2：使用反射获取Method对象
            String methodName = request.getMethodName();
            Class<?>[] paramTypes = request.getParamTypes();
            // getMethod()会在类及其父类、接口中查找匹配的public方法
            Method method = serviceImpl.getClass().getMethod(methodName, paramTypes);
            
            // 步骤3：使用反射调用实际方法
            Object[] parameters = request.getParameters();
            // invoke(对象实例, 参数数组) 返回方法执行结果
            Object result = method.invoke(serviceImpl, parameters);
            
            // 步骤4：将结果封装成成功的响应对象
            return RpcResponse.success(result);
            
        } catch (Exception e) {
            // 捕获所有异常，包括：
            // - NoSuchMethodException: 方法不存在
            // - IllegalAccessException: 方法访问权限不足
            // - InvocationTargetException: 方法执行过程中抛出异常
            System.err.println("[RpcServer] Error processing request: " + e.getMessage());
            e.printStackTrace();
            // 返回失败响应
            return RpcResponse.fail("Server error: " + e.getMessage());
        }
    }
    
    /**
     * 关闭服务器并释放资源
     * Shutdown the server and release resources
     * 
     * 该方法负责优雅地关闭服务器，释放所有占用的资源
     * 关闭流程：
     * 1. 检查服务器是否正在运行
     * 2. 设置运行标志为false，停止接收新连接
     * 3. 关闭线程池，等待所有任务执行完成
     * 4. 打印关闭日志
     * 
     * 注意：该方法会在start()方法的finally块中被调用，
     * 确保无论服务器如何退出都能正确释放资源
     */
    public void shutdown() {
        // 步骤1：检查服务器是否正在运行
        if (!running) {
            return;
        }
        
        // 步骤2：设置运行标志为false
        // 这会导致start()方法中的while循环退出
        running = false;
        System.out.println("[RpcServer] Shutting down RPC server...");
        
        // 步骤3：关闭线程池
        if (executorService != null && !executorService.isShutdown()) {
            // shutdown()会停止接收新任务，但会等待已提交的任务执行完成
            // 如果需要立即停止，可以使用shutdownNow()
            executorService.shutdown();
        }
        
        // 步骤4：打印关闭完成日志
        System.out.println("[RpcServer] RPC server stopped");
    }
}

