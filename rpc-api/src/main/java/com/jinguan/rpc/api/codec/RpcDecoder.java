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
 * RPC 消息解码器 - 将字节流解码为RpcMessage对象
 * RPC Message Decoder
 * 
 * 该解码器负责将网络传输的字节流按照自定义协议格式解码为RpcMessage对象
 * 
 * 继承关系：
 * - LengthFieldBasedFrameDecoder: Netty提供的基于长度字段的帧解码器
 * - 自动处理TCP粘包拆包问题
 * - 我们只需要实现具体的协议解析逻辑
 * 
 * 工作原理：
 * 1. LengthFieldBasedFrameDecoder根据长度字段切分完整的消息帧
 * 2. decode方法接收到完整的消息帧
 * 3. decodeFrame方法按照协议格式解析各个字段
 * 4. 使用序列化器反序列化消息体
 * 5. 组装成RpcMessage对象返回
 * 
 * @author JinGuan
 * @version 2.0.0
 */
public class RpcDecoder extends LengthFieldBasedFrameDecoder {
    
    private static final Logger logger = LoggerFactory.getLogger(RpcDecoder.class);
    
    /**
     * 无参构造函数 - 使用默认参数
     * Default constructor
     * 
     * 调用父类构造函数，配置基于长度字段的帧解码参数：
     * - maxFrameLength: 16MB（最大消息长度）
     * - lengthFieldOffset: 7（长度字段偏移量 = 魔数4 + 版本1 + 序列化类型1 + 消息类型1）
     * - lengthFieldLength: 4（长度字段本身占4字节）
     * - lengthAdjustment: 0（长度字段后无需调整）
     * - initialBytesToStrip: 0（不跳过任何字节，我们需要完整的数据）
     */
    public RpcDecoder() {
        this(RpcProtocol.MAX_FRAME_LENGTH, 7, 4, 0, 0);
    }
    
    /**
     * 有参构造函数 - 自定义帧解码参数
     * Constructor with custom parameters
     * 
     * LengthFieldBasedFrameDecoder参数说明：
     * @param maxFrameLength 最大帧长度，超过此长度会抛出异常
     * @param lengthFieldOffset 长度字段的偏移量（从哪个位置开始是长度字段）
     * @param lengthFieldLength 长度字段的字节数（长度字段占多少字节）
     * @param lengthAdjustment 长度调整值（长度字段之后还有多少字节不计入长度）
     * @param initialBytesToStrip 跳过的初始字节数（解码时跳过前几个字节）
     */
    public RpcDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, 
                     int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }
    
    /**
     * 解码方法 - Netty框架调用的入口方法
     * Decode method - entry point called by Netty framework
     * 
     * 工作流程：
     * 1. 调用父类decode方法（LengthFieldBasedFrameDecoder）
     * 2. 父类会根据长度字段切分出完整的消息帧
     * 3. 我们接收到完整的ByteBuf后，调用decodeFrame进行具体解析
     * 4. 确保ByteBuf被正确释放，避免内存泄漏
     * 
     * @param ctx Netty的Channel上下文
     * @param in 输入的字节缓冲区
     * @return 解码后的对象（RpcMessage或null）
     * @throws Exception 解码失败时抛出异常
     */
    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        // 步骤1：调用父类的decode方法
        // 父类会根据长度字段切分出一个完整的消息帧
        Object decoded = super.decode(ctx, in);
        
        // 步骤2：判断是否成功解码出完整帧
        if (decoded instanceof ByteBuf) {
            ByteBuf frame = (ByteBuf) decoded;
            try {
                // 步骤3：解析具体的协议内容
                return decodeFrame(frame);
            } finally {
                // 步骤4：释放ByteBuf，防止内存泄漏
                // Netty使用引用计数管理ByteBuf，使用后必须释放
                frame.release();
            }
        }
        return decoded;
    }
    
    /**
     * 解析消息帧 - 按照协议格式解析字节数据
     * Decode frame - parses byte data according to protocol format
     * 
     * 解析步骤（8步）：
     * 1. 检查是否有足够的字节用于协议头
     * 2. 读取并验证魔数
     * 3. 读取并验证版本号
     * 4. 读取序列化类型
     * 5. 读取消息类型
     * 6. 读取消息体长度
     * 7. 读取消息体字节数组
     * 8. 反序列化消息体并组装RpcMessage对象
     * 
     * @param in 包含完整消息帧的字节缓冲区
     * @return 解码后的RpcMessage对象
     */
    private Object decodeFrame(ByteBuf in) {
        try {
            // 步骤1：检查字节数是否足够
            // 至少需要11字节的协议头
            if (in.readableBytes() < RpcProtocol.HEADER_LENGTH) {
                logger.warn("Not enough bytes for header");
                return null;
            }
            
            // 步骤2：读取并验证魔数（4字节）
            // 如果魔数不匹配，说明不是有效的RPC消息
            int magicNumber = in.readInt();
            if (magicNumber != RpcProtocol.MAGIC_NUMBER) {
                throw new IllegalArgumentException("Invalid magic number: " + Integer.toHexString(magicNumber));
            }
            
            // 步骤3：读取并验证版本号（1字节）
            // 如果版本不支持，拒绝处理
            byte version = in.readByte();
            if (version != RpcProtocol.VERSION) {
                throw new IllegalArgumentException("Unsupported version: " + version);
            }
            
            // 步骤4：读取序列化类型（1字节）
            // 用于确定使用哪个序列化器反序列化消息体
            byte serializerType = in.readByte();
            
            // 步骤5：读取消息类型（1字节）
            // 用于确定消息体的Java类型（RpcRequest/RpcResponse/String）
            byte messageType = in.readByte();
            
            // 步骤6：读取消息体长度（4字节）
            int length = in.readInt();
            
            // 步骤7：读取消息体（N字节）
            byte[] bodyBytes = new byte[length];
            in.readBytes(bodyBytes);
            
            // 步骤8：反序列化消息体
            Object data = null;
            if (length > 0) {
                // 获取对应的序列化器
                Serializer serializer = SerializerFactory.getSerializer(serializerType);
                
                // 根据消息类型确定要反序列化的Java类型
                Class<?> clazz = getClassByMessageType(messageType);
                
                // 执行反序列化，将字节数组转换为Java对象
                data = serializer.deserialize(bodyBytes, clazz);
            }
            
            // 步骤9：组装RpcMessage对象
            RpcMessage message = new RpcMessage();
            message.setSerializerType(serializerType);
            message.setMessageType(messageType);
            message.setData(data);
            
            // 记录解码日志
            logger.debug("Decoded message: type={}, length={}", messageType, length);
            
            return message;
            
        } catch (Exception e) {
            // 捕获解码过程中的异常
            logger.error("Decode frame failed", e);
            throw new RuntimeException("Decode failed", e);
        }
    }
    
    /**
     * 根据消息类型获取对应的Java类
     * Get Java class by message type
     * 
     * 不同的消息类型对应不同的Java类：
     * - REQUEST -> RpcRequest.class
     * - RESPONSE -> RpcResponse.class
     * - HEARTBEAT -> String.class
     * 
     * 这样序列化器就知道要反序列化成什么类型的对象
     * 
     * @param messageType 消息类型字节
     * @return 对应的Java类
     */
    private Class<?> getClassByMessageType(byte messageType) {
        switch (messageType) {
            case RpcProtocol.MessageType.REQUEST:
                return RpcRequest.class;
            case RpcProtocol.MessageType.RESPONSE:
                return RpcResponse.class;
            case RpcProtocol.MessageType.HEARTBEAT_REQUEST:
            case RpcProtocol.MessageType.HEARTBEAT_RESPONSE:
                // 心跳消息使用简单的字符串
                return String.class;
            default:
                throw new IllegalArgumentException("Unknown message type: " + messageType);
        }
    }
}

