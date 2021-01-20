package io.tt.tmq.core.partition;

import lombok.Data;

@Data
public class AppendMessageResult {
    // 返回码
    private AppendMessageStatus status;
    /**
     * 消息的绝对偏移量，当一个队列/分区的的当前日志段文件剩余空间无法放入一条消息时，
     * status == END_OF_FILE, absPosition可以作为下一个新建日志段文件的文件名，用于标识
     * 这个新日志段文件的起始位置在当前分区中的绝对偏移量。同时当前日志段文件标记为已满。
     *
     * 当为负数的时候，表示一个错误，具体错误由AppendMessageStatus表示。
     */
    private long absPosition;

    /**
     * 当前日志文件内的偏移量.
     *
     * 当为负数的时候，表示一个错误，具体错误由AppendMessageStatus表示。
     */
    private long posInCurrentLog;

    /**
     * 添加的消息大小
     */
    private int msgLen;

    public AppendMessageResult(AppendMessageStatus status, long absPosition, long posInCurrentLog, int msgLen) {
        this.status = status;
        this.absPosition = absPosition;
        this.posInCurrentLog = posInCurrentLog;
        this.msgLen = msgLen;
    }
}
