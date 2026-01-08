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
 * RPC 消息编码器 - 将RpcMessage对象编码为字节流
 * RPC Message Encoder
 * 
 * 该编码器负责将RpcMessage对象按照自定义协议格式编码为字节流，以便通过网络传输
 * 
 * 编码后的协议格式：
 * +--------+--------+------------+-------------+--------+---------+
 * | Magic  |Version |Serializer  | MessageType |Length  |  Body   |
 * | 4字节  | 1字节  |  1字节     |  1字节      | 4字节  | N字节   |
 * +--------+--------+------------+-------------+--------+---------+
 * 
 * 工作原理：
 * 1. Netty会自动调用encode方法处理出站消息
 * 2. 编码器按照协议格式依次写入各个字段
 * 3. 使用可插拔的序列化器序列化消息体
 * 4. 将完整的字节数据写入ByteBuf
 * 
 * 继承关系：
 * - MessageToByteEncoder<RpcMessage>: Netty提供的泛型编码器基类
 * - 只需实现encode方法即可完成编码逻辑
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class RpcEncoder extends MessageToByteEncoder<RpcMessage> {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcEncoder.class);
    
    /**
     * 编码方法 - 将RpcMessage编码为字节流
     * Encode method - encodes RpcMessage into bytes
     * 
     * 该方法由Netty框架自动调用，当有RpcMessage类型的对象需要发送时触发
     * 
     * 编码步骤（7步）：
     * 1. 写入魔数（4字节）- 用于协议识别
     * 2. 写入版本号（1字节）- 支持协议版本管理
     * 3. 写入序列化类型（1字节）- 标识使用的序列化算法
     * 4. 写入消息类型（1字节）- 区分请求/响应/心跳
     * 5. 序列化消息体 - 将data对象序列化为字节数组
     * 6. 写入消息体长度（4字节）- 用于解决TCP粘包拆包
     * 7. 写入消息体（N字节）- 实际的业务数据
     * 
     * @param ctx Netty的Channel上下文，用于获取Channel信息
     * @param msg 要编码的RpcMessage对象
     * @param out Netty的字节缓冲区，编码后的数据写入该缓冲区
     * @throws Exception 编码失败时抛出异常
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage msg, ByteBuf out) throws Exception {
        try {
            // 步骤1：写入魔数（4字节）
            // 魔数用于快速识别是否为有效的RPC协议消息
            out.writeInt(RpcProtocol.MAGIC_NUMBER);
            
            // 步骤2：写入协议版本号（1字节）
            // 版本号用于协议升级和兼容性处理
            out.writeByte(RpcProtocol.VERSION);
            
            // 步骤3：写入序列化类型（1字节）
            // 告诉接收方使用哪种序列化算法反序列化消息体
            out.writeByte(msg.getSerializerType());
            
            // 步骤4：写入消息类型（1字节）
            // 标识这是请求、响应还是心跳消息
            out.writeByte(msg.getMessageType());
            
            // 步骤5：序列化消息体
            // 根据指定的序列化类型，将data对象序列化为字节数组
            byte[] bodyBytes = null;
            if (msg.getData() != null) {
                // 从工厂获取对应的序列化器
                Serializer serializer = SerializerFactory.getSerializer(msg.getSerializerType());
                // 执行序列化，将对象转换为字节数组
                bodyBytes = serializer.serialize(msg.getData());
            }
            
            // 步骤6：写入消息体长度（4字节）
            // 接收方通过长度字段知道消息体有多少字节
            // 这是解决TCP粘包拆包问题的关键
            int length = bodyBytes != null ? bodyBytes.length : 0;
            out.writeInt(length);
            
            // 步骤7：写入消息体（N字节）
            // 将序列化后的字节数组写入缓冲区
            if (bodyBytes != null && bodyBytes.length > 0) {
                out.writeBytes(bodyBytes);
            }
            
            // 记录编码日志，便于调试
            logger.debug("Encoded message: type={}, length={}", msg.getMessageType(), length);
            
        } catch (Exception e) {
            // 捕获编码过程中的异常并记录日志
            logger.error("Encode message failed", e);
            throw e;
        }
    }
}

