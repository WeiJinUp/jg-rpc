package com.jinguan.rpc.api.serializer;

import com.jinguan.rpc.api.protocol.RpcProtocol;

import java.io.*;

/**
 * Java native serialization implementation
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class JavaSerializer implements Serializer {
    
    @Override
    public byte[] serialize(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            oos.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Java serialization failed", e);
        }
    }
    
    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return clazz.cast(ois.readObject());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Java deserialization failed", e);
        }
    }
    
    @Override
    public byte getType() {
        return RpcProtocol.SerializerType.JAVA;
    }
}

