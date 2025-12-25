package com.jinguan.rpc.api.codec;

import com.jinguan.rpc.api.dto.RpcRequest;
import com.jinguan.rpc.api.dto.RpcResponse;
import com.jinguan.rpc.api.protocol.RpcMessage;
import com.jinguan.rpc.api.protocol.RpcProtocol;
import com.jinguan.rpc.api.serializer.Serializer;
import com.jinguan.rpc.api.serializer.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC Message Decoder
 * 
 * Decodes bytes into RpcMessage according to custom protocol
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class RpcDecoder extends LengthFieldBasedFrameDecoder {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcDecoder.class);
    
    public RpcDecoder() {
        this(RpcProtocol.MAX_FRAME_LENGTH, 7, 4, 0, 0);
    }
    
    public RpcDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, 
                     int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }
    
    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object decoded = super.decode(ctx, in);
        if (decoded instanceof ByteBuf) {
            ByteBuf frame = (ByteBuf) decoded;
            try {
                return decodeFrame(frame);
            } finally {
                frame.release();
            }
        }
        return decoded;
    }
    
    private Object decodeFrame(ByteBuf in) {
        try {
            // Check if we have enough bytes for the header
            if (in.readableBytes() < RpcProtocol.HEADER_LENGTH) {
                logger.warn("Not enough bytes for header");
                return null;
            }
            
            // 1. Read and check magic number
            int magicNumber = in.readInt();
            if (magicNumber != RpcProtocol.MAGIC_NUMBER) {
                throw new IllegalArgumentException("Invalid magic number: " + Integer.toHexString(magicNumber));
            }
            
            // 2. Read and check version
            byte version = in.readByte();
            if (version != RpcProtocol.VERSION) {
                throw new IllegalArgumentException("Unsupported version: " + version);
            }
            
            // 3. Read serializer type
            byte serializerType = in.readByte();
            
            // 4. Read message type
            byte messageType = in.readByte();
            
            // 5. Read body length
            int length = in.readInt();
            
            // 6. Read body
            byte[] bodyBytes = new byte[length];
            in.readBytes(bodyBytes);
            
            // 7. Deserialize body
            Object data = null;
            if (length > 0) {
                Serializer serializer = SerializerFactory.getSerializer(serializerType);
                
                // Determine the class type based on message type
                Class<?> clazz = getClassByMessageType(messageType);
                data = serializer.deserialize(bodyBytes, clazz);
            }
            
            // 8. Create RpcMessage
            RpcMessage message = new RpcMessage();
            message.setSerializerType(serializerType);
            message.setMessageType(messageType);
            message.setData(data);
            
            logger.debug("Decoded message: type={}, length={}", messageType, length);
            
            return message;
            
        } catch (Exception e) {
            logger.error("Decode frame failed", e);
            throw new RuntimeException("Decode failed", e);
        }
    }
    
    private Class<?> getClassByMessageType(byte messageType) {
        switch (messageType) {
            case RpcProtocol.MessageType.REQUEST:
                return RpcRequest.class;
            case RpcProtocol.MessageType.RESPONSE:
                return RpcResponse.class;
            case RpcProtocol.MessageType.HEARTBEAT_REQUEST:
            case RpcProtocol.MessageType.HEARTBEAT_RESPONSE:
                return String.class; // Heartbeat uses simple string
            default:
                throw new IllegalArgumentException("Unknown message type: " + messageType);
        }
    }
}

