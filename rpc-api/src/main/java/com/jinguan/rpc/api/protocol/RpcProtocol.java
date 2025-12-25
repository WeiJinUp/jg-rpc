package com.jinguan.rpc.api.protocol;

/**
 * RPC Protocol Constants
 * 
 * Protocol format: [Magic Number(4B)][Version(1B)][Serializer(1B)][MessageType(1B)][Length(4B)][Body]
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class RpcProtocol {
    
    /**
     * Magic number for protocol identification (0xCAFEBABE)
     */
    public static final int MAGIC_NUMBER = 0xCAFEBABE;
    
    /**
     * Protocol version
     */
    public static final byte VERSION = 1;
    
    /**
     * Header length (bytes)
     * Magic(4) + Version(1) + Serializer(1) + MessageType(1) + Length(4) = 11 bytes
     */
    public static final int HEADER_LENGTH = 11;
    
    /**
     * Maximum frame length (16MB)
     */
    public static final int MAX_FRAME_LENGTH = 16 * 1024 * 1024;
    
    /**
     * Serialization algorithms
     */
    public static class SerializerType {
        public static final byte JAVA = 0;
        public static final byte JSON = 1;
        public static final byte PROTOBUF = 2;
        public static final byte HESSIAN = 3;
    }
    
    /**
     * Message types
     */
    public static class MessageType {
        public static final byte REQUEST = 1;
        public static final byte RESPONSE = 2;
        public static final byte HEARTBEAT_REQUEST = 3;
        public static final byte HEARTBEAT_RESPONSE = 4;
    }
}

