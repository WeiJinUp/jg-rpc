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
 * RPC Server - handles service registration and request processing
 * Uses native Java Socket and reflection for RPC implementation
 *
 * @author JinGuan
 * @version 1.0.0
 */
public class RpcServer {
    
    /**
     * Default server port
     */
    private static final int DEFAULT_PORT = 8888;
    
    /**
     * Service registry - maps interface name to service implementation instance
     */
    private final Map<String, Object> serviceMap = new HashMap<>();
    
    /**
     * Thread pool for handling client requests
     */
    private final ExecutorService executorService;
    
    /**
     * Server port
     */
    private final int port;
    
    /**
     * Server running flag
     */
    private volatile boolean running = false;
    
    public RpcServer() {
        this(DEFAULT_PORT);
    }
    
    public RpcServer(int port) {
        this.port = port;
        // Create a fixed thread pool with available processors * 2
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    }
    
    /**
     * Register a service implementation
     * The first interface of the service implementation will be used as the service name
     *
     * @param service the service implementation instance
     */
    public void register(Object service) {
        if (service == null) {
            throw new IllegalArgumentException("Service cannot be null");
        }
        
        Class<?>[] interfaces = service.getClass().getInterfaces();
        if (interfaces.length == 0) {
            throw new IllegalArgumentException("Service must implement at least one interface");
        }
        
        // Register the first interface
        String interfaceName = interfaces[0].getName();
        serviceMap.put(interfaceName, service);
        System.out.println("[RpcServer] Registered service: " + interfaceName);
    }
    
    /**
     * Start the RPC server
     */
    public void start() {
        if (running) {
            System.out.println("[RpcServer] Server is already running on port " + port);
            return;
        }
        
        running = true;
        System.out.println("[RpcServer] Starting RPC server on port " + port + "...");
        
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[RpcServer] RPC server started successfully, listening on port " + port);
            
            while (running) {
                // Accept client connection
                Socket clientSocket = serverSocket.accept();
                System.out.println("[RpcServer] Accepted connection from " + clientSocket.getRemoteSocketAddress());
                
                // Handle request in thread pool
                executorService.execute(() -> handleRequest(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("[RpcServer] Server error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }
    
    /**
     * Handle client request
     *
     * @param clientSocket the client socket
     */
    private void handleRequest(Socket clientSocket) {
        try (ObjectInputStream input = new ObjectInputStream(clientSocket.getInputStream());
             ObjectOutputStream output = new ObjectOutputStream(clientSocket.getOutputStream())) {
            
            // Read RPC request
            RpcRequest request = (RpcRequest) input.readObject();
            System.out.println("[RpcServer] Received request: " + request);
            
            // Process request and get response
            RpcResponse response = processRequest(request);
            
            // Send response back to client
            output.writeObject(response);
            output.flush();
            System.out.println("[RpcServer] Sent response: " + response);
            
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[RpcServer] Error handling request: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("[RpcServer] Error closing client socket: " + e.getMessage());
            }
        }
    }
    
    /**
     * Process RPC request using reflection
     *
     * @param request the RPC request
     * @return RPC response
     */
    private RpcResponse processRequest(RpcRequest request) {
        try {
            // Get service instance from registry
            String interfaceName = request.getInterfaceName();
            Object serviceImpl = serviceMap.get(interfaceName);
            
            if (serviceImpl == null) {
                return RpcResponse.fail("Service not found: " + interfaceName);
            }
            
            // Get method using reflection
            String methodName = request.getMethodName();
            Class<?>[] paramTypes = request.getParamTypes();
            Method method = serviceImpl.getClass().getMethod(methodName, paramTypes);
            
            // Invoke method
            Object[] parameters = request.getParameters();
            Object result = method.invoke(serviceImpl, parameters);
            
            return RpcResponse.success(result);
            
        } catch (Exception e) {
            System.err.println("[RpcServer] Error processing request: " + e.getMessage());
            e.printStackTrace();
            return RpcResponse.fail("Server error: " + e.getMessage());
        }
    }
    
    /**
     * Shutdown the server and release resources
     */
    public void shutdown() {
        if (!running) {
            return;
        }
        
        running = false;
        System.out.println("[RpcServer] Shutting down RPC server...");
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        System.out.println("[RpcServer] RPC server stopped");
    }
}

