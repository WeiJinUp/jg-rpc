package com.jinguan.rpc.api.serializer;

import com.jinguan.rpc.api.protocol.RpcProtocol;

import java.util.HashMap;
import java.util.Map;

/**
 * Serializer Factory - manages serializer instances
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class SerializerFactory {
    
    private static final Map<Byte, Serializer> SERIALIZER_MAP = new HashMap<>();
    
    static {
        // Register default serializers
        register(new JavaSerializer());
        register(new JsonSerializer());
    }
    
    /**
     * Register a serializer
     * 
     * @param serializer serializer instance
     */
    public static void register(Serializer serializer) {
        SERIALIZER_MAP.put(serializer.getType(), serializer);
    }
    
    /**
     * Get serializer by type
     * 
     * @param type serializer type
     * @return serializer instance
     */
    public static Serializer getSerializer(byte type) {
        Serializer serializer = SERIALIZER_MAP.get(type);
        if (serializer == null) {
            throw new IllegalArgumentException("Unsupported serializer type: " + type);
        }
        return serializer;
    }
    
    /**
     * Get default serializer (JSON)
     * 
     * @return default serializer
     */
    public static Serializer getDefaultSerializer() {
        return getSerializer(RpcProtocol.SerializerType.JSON);
    }
}

