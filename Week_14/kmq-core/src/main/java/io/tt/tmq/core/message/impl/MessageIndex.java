package io.tt.tmq.core.message.impl;

import io.tt.tmq.core.message.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;

/**
 * 消息的索引，可以看做时一种特殊的消息，也利用日志段TQLog保存.
 * 保存了某条消息在分区中的日志段的编号、消息在日志段内的偏移。
 * 所有的MessageIndex消息的大小是固定的，一个接一个的放在日志文件内，自身的序号对应消息的序号。
 * 因为索引项很小，所以一个分区对应一个索引文件。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageIndex implements Message {
    //消息所在日志段的文件位于TQQueue中的序号，从0开始计数
    private int numTQLog;
    //消息在日志段内的偏移量
    private int posInTQLog;
    //消息的绝对偏移
    private long absPos;
    //对应的消息大小
    private int msgLen;

    //自身的大小
    public static final int MESSAGE_INDEX_SIZE = 8  //消息所在日志段的文件位于TQQueue中的序号
                                                + 8  //消息在日志段内的偏移量
                                                + 8; //消息的绝对偏移

    @Override
    public int calMsgLengthInByte() {
        return MESSAGE_INDEX_SIZE;
    }

    /**
     * 序列化到缓冲区，因为索引项的大小是固定的，所以在序列化时就不保存本身的大小了。
     * @param byteBuffer 缓冲区
     */
    @Override
    public void serializeTo(ByteBuffer byteBuffer) {
        byteBuffer.putInt(numTQLog);
        byteBuffer.putInt(posInTQLog);
        byteBuffer.putLong(absPos);
        byteBuffer.putInt(msgLen);
    }

    @Override
    public void deSerializeFrom(ByteBuffer byteBuffer) {
        numTQLog = byteBuffer.getInt();
        posInTQLog = byteBuffer.getInt();
        absPos = byteBuffer.getLong();
        msgLen = byteBuffer.getInt();
    }
}
