package io.tt.tmq.core.message;

import java.io.Serializable;
import java.nio.ByteBuffer;

public interface Message extends Serializable {
    /**
     * 得到消息的长度
     * @return 消息长度
     */
    int calMsgLengthInByte();

    /**
     * 将消息序列化到指定的缓冲区
     * @param byteBuffer 缓冲区
     */
    void serializeTo(ByteBuffer byteBuffer);

    /**
     * 从缓冲区反序列化
     * @param byteBuffer
     */
    void deSerializeFrom(ByteBuffer byteBuffer);
}
