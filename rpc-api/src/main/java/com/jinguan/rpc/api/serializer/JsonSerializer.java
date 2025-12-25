package com.jinguan.rpc.api.serializer;

import com.google.gson.*;
import com.jinguan.rpc.api.protocol.RpcProtocol;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * JSON serialization implementation using Gson
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class JsonSerializer implements Serializer {
    
    private final Gson gson;
    
    public JsonSerializer() {
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .registerTypeAdapter(Class.class, new ClassCodec())
                .create();
    }
    
    /**
     * Custom TypeAdapter for Class serialization
     */
    private static class ClassCodec implements com.google.gson.JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {
        @Override
        public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
            // Serialize Class to its name
            return new JsonPrimitive(src.getName());
        }
        
        @Override
        public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                // Deserialize from class name
                return Class.forName(json.getAsString());
            } catch (ClassNotFoundException e) {
                throw new JsonParseException("Class not found: " + json.getAsString(), e);
            }
        }
    }
    
    @Override
    public byte[] serialize(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            String json = gson.toJson(obj);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }
    
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        
        try {
            String json = new String(bytes, StandardCharsets.UTF_8);
            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("JSON deserialization failed", e);
        }
    }
    
    @Override
    public byte getType() {
        return RpcProtocol.SerializerType.JSON;
    }
}

