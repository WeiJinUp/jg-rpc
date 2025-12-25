package com.jinguan.rpc.api.codec;

import com.jinguan.rpc.api.protocol.RpcMessage;
import com.jinguan.rpc.api.protocol.RpcProtocol;
import com.jinguan.rpc.api.serializer.Serializer;
import com.jinguan.rpc.api.serializer.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC Message Encoder
 * 
 * Encodes RpcMessage into bytes according to custom protocol:
 * +--------+--------+------------+-------------+--------+---------+
 * | Magic  |Version |Serializer  | MessageType |Length  |  Body   |
 * | 4bytes | 1byte  |  1byte     |  1byte      | 4bytes |         |
 * +--------+--------+------------+-------------+--------+---------+
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class RpcEncoder extends MessageToByteEncoder<RpcMessage> {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcEncoder.class);
    
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage msg, ByteBuf out) throws Exception {
        try {
            // 1. Write magic number (4 bytes)
            out.writeInt(RpcProtocol.MAGIC_NUMBER);
            
            // 2. Write version (1 byte)
            out.writeByte(RpcProtocol.VERSION);
            
            // 3. Write serializer type (1 byte)
            out.writeByte(msg.getSerializerType());
            
            // 4. Write message type (1 byte)
            out.writeByte(msg.getMessageType());
            
            // 5. Serialize body
            byte[] bodyBytes = null;
            if (msg.getData() != null) {
                Serializer serializer = SerializerFactory.getSerializer(msg.getSerializerType());
                bodyBytes = serializer.serialize(msg.getData());
            }
            
            // 6. Write body length (4 bytes)
            int length = bodyBytes != null ? bodyBytes.length : 0;
            out.writeInt(length);
            
            // 7. Write body
            if (bodyBytes != null && bodyBytes.length > 0) {
                out.writeBytes(bodyBytes);
            }
            
            logger.debug("Encoded message: type={}, length={}", msg.getMessageType(), length);
            
        } catch (Exception e) {
            logger.error("Encode message failed", e);
            throw e;
        }
    }
}

