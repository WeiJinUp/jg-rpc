package com.jinguan.rpc.client;

import com.jinguan.rpc.api.dto.RpcRequest;
import com.jinguan.rpc.api.dto.RpcResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * RPC Client - sends RPC requests to server and receives responses
 * Uses native Java Socket for network communication
 *
 * @author JinGuan
 * @version 1.0.0
 */
public class RpcClient {
    
    /**
     * Server host
     */
    private final String host;
    
    /**
     * Server port
     */
    private final int port;
    
    /**
     * Default connection timeout (milliseconds)
     */
    private static final int DEFAULT_TIMEOUT = 5000;
    
    public RpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    /**
     * Send RPC request to server and get response
     *
     * @param request the RPC request
     * @return RPC response
     * @throws Exception if communication fails
     */
    public RpcResponse sendRequest(RpcRequest request) throws Exception {
        Socket socket = null;
        ObjectOutputStream output = null;
        ObjectInputStream input = null;
        
        try {
            // Connect to server
            System.out.println("[RpcClient] Connecting to server at " + host + ":" + port);
            socket = new Socket(host, port);
            socket.setSoTimeout(DEFAULT_TIMEOUT);
            
            // Send request
            output = new ObjectOutputStream(socket.getOutputStream());
            output.writeObject(request);
            output.flush();
            System.out.println("[RpcClient] Sent request: " + request);
            
            // Receive response
            input = new ObjectInputStream(socket.getInputStream());
            RpcResponse response = (RpcResponse) input.readObject();
            System.out.println("[RpcClient] Received response: " + response);
            
            return response;
            
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[RpcClient] Error sending request: " + e.getMessage());
            throw new Exception("RPC call failed: " + e.getMessage(), e);
        } finally {
            // Close resources
            closeQuietly(input);
            closeQuietly(output);
            closeQuietly(socket);
        }
    }
    
    /**
     * Helper method to close resources quietly
     */
    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
    
    /**
     * Close socket quietly
     */
    private void closeQuietly(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}

