package com.jinguan.rpc.api.serializer;

/**
 * Serializer interface - pluggable serialization mechanism
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public interface Serializer {
    
    /**
     * Serialize object to byte array
     * 
     * @param obj object to serialize
     * @return serialized bytes
     */
    byte[] serialize(Object obj);
    
    /**
     * Deserialize byte array to object
     * 
     * @param bytes serialized bytes
     * @param clazz target class
     * @param <T> type parameter
     * @return deserialized object
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);
    
    /**
     * Get serializer algorithm type
     * 
     * @return algorithm type code
     */
    byte getType();
}

