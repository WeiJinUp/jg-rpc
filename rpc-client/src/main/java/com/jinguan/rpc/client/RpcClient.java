package com.jinguan.rpc.client;

import com.jinguan.rpc.api.dto.RpcRequest;
import com.jinguan.rpc.api.dto.RpcResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * RPC 客户端 - 向服务器发送RPC请求并接收响应
 * RPC Client - sends RPC requests to server and receives responses
 * 
 * 这是RPC框架的客户端组件，主要功能包括：
 * 1. 建立与服务器的Socket连接
 * 2. 将RpcRequest对象序列化并发送到服务器
 * 3. 接收服务器返回的RpcResponse对象并反序列化
 * 4. 管理网络连接和资源释放
 * 
 * Uses native Java Socket for network communication
 *
 * @author JinGuan
 * @version 1.0.0
 */
public class RpcClient {
    
    /**
     * 服务器主机地址
     * Server host address
     */
    private final String host;
    
    /**
     * 服务器端口号
     * Server port number
     */
    private final int port;
    
    /**
     * 默认连接超时时间（毫秒）
     * 设置超时可以避免客户端无限期等待
     * Default connection timeout (milliseconds)
     */
    private static final int DEFAULT_TIMEOUT = 5000;
    
    /**
     * 构造函数 - 创建RPC客户端实例
     * Constructor - creates an RPC client instance
     * 
     * @param host 服务器主机地址（如："localhost"或"192.168.1.100"）
     * @param port 服务器端口号（如：8888）
     */
    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    /**
     * 发送RPC请求到服务器并获取响应
     * Send RPC request to server and get response
     * 
     * 这是客户端的核心方法，完成一次完整的RPC调用
     * 执行流程：
     * 1. 建立与服务器的Socket连接
     * 2. 设置Socket超时时间，避免无限等待
     * 3. 将RpcRequest对象序列化并发送到服务器
     * 4. 接收服务器返回的RpcResponse对象并反序列化
     * 5. 关闭所有资源（输入流、输出流、Socket）
     * 
     * 注意事项：
     * - 该方法是同步阻塞的，会等待服务器响应
     * - 每次调用都会建立新的连接（第一阶段实现）
     * - 第二阶段会引入连接池和长连接优化
     *
     * @param request RPC请求对象，包含要调用的接口、方法、参数等信息
     * @return RPC响应对象，包含方法执行结果或错误信息
     * @throws Exception 如果通信失败（如网络异常、超时等）
     */
    public RpcResponse sendRequest(RpcRequest request) throws Exception {
        Socket socket = null;
        ObjectOutputStream output = null;
        ObjectInputStream input = null;
        
        try {
            // 步骤1：建立与服务器的Socket连接
            System.out.println("[RpcClient] Connecting to server at " + host + ":" + port);
            socket = new Socket(host, port);
            
            // 步骤2：设置Socket读取超时时间
            // 如果在指定时间内没有收到响应，会抛出SocketTimeoutException
            socket.setSoTimeout(DEFAULT_TIMEOUT);
            
            // 步骤3：创建输出流并发送请求
            output = new ObjectOutputStream(socket.getOutputStream());
            // 将RpcRequest对象序列化为字节流
            output.writeObject(request);
            // 强制刷新缓冲区，确保数据立即发送
            output.flush();
            System.out.println("[RpcClient] Sent request: " + request);
            
            // 步骤4：创建输入流并接收响应
            input = new ObjectInputStream(socket.getInputStream());
            // 从字节流反序列化为RpcResponse对象
            // 这是一个阻塞操作，会等待服务器响应
            RpcResponse response = (RpcResponse) input.readObject();
            System.out.println("[RpcClient] Received response: " + response);
            
            // 返回响应对象
            return response;
            
        } catch (IOException | ClassNotFoundException e) {
            // 捕获异常，包括：
            // - IOException: 网络连接失败、读写超时等
            // - ClassNotFoundException: 反序列化时找不到类
            System.err.println("[RpcClient] Error sending request: " + e.getMessage());
            throw new Exception("RPC call failed: " + e.getMessage(), e);
        } finally {
            // 步骤5：无论是否发生异常，都要关闭所有资源
            // 按照打开的相反顺序关闭
            closeQuietly(input);
            closeQuietly(output);
            closeQuietly(socket);
        }
    }
    
    /**
     * 静默关闭资源 - 关闭AutoCloseable资源（如输入输出流）
     * Helper method to close resources quietly
     * 
     * 该方法用于安全地关闭资源，即使关闭过程中出现异常也不会影响程序
     * 这是一个常见的工具方法，避免在finally块中编写大量的异常处理代码
     * 
     * 为什么要"静默"关闭？
     * - 在finally块中，主要目标是释放资源
     * - 如果关闭失败，通常无法进行有效的错误处理
     * - 避免finally块中的异常掩盖try块中的主异常
     *
     * @param closeable 要关闭的AutoCloseable对象（如输入流、输出流）
     */
    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                // 静默忽略关闭异常
                // 在实际生产环境中，可以考虑记录日志
            }
        }
    }
    
    /**
     * 静默关闭Socket连接
     * Close socket quietly
     * 
     * 专门用于关闭Socket连接的方法
     * Socket需要单独处理，因为它有isClosed()状态检查
     * 
     * @param socket 要关闭的Socket对象
     */
    private void closeQuietly(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                // 静默忽略关闭异常
            }
        }
    }
}

