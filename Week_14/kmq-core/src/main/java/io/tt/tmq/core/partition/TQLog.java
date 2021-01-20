package io.tt.tmq.core.partition;

import io.tt.tmq.core.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消息的物理容器，相当于KAFKA中的分区或者RocketMQ中队列的底层支持。参考了RocketMQ的MappedFile。
 * 但是概念很多来自于KAFKA。
 *
 * 非线程安全的类！需要外部的锁进行保护
 */
@Slf4j
public class TQLog {
    /**
     * 每个TQLog文件的最大大小，这里临时指定为1G。
     * 每个文件不一定会写满，当剩余空间容纳不下一条消息时，就会新生成一个日志段文件。
     */
    public final static int MAX_LOG_SIZE = 1 * 1024 * 1024 * 1024;

    //最大的消息大小
    public static final int MAX_MESSAGE_SIZE = 10 * 1024 * 1024;

    //本日志段内的写入偏移量
    protected final AtomicInteger wrotePosition = new AtomicInteger(0);

    //当前日志段是否可写
    private final AtomicBoolean full = new AtomicBoolean(false);

    protected int fileSize;
    protected FileChannel fileChannel;

    //filename是所有消息的起始位移，一个数字
    private String fileName;

    /**
     * 本日志段的起始位置在所有消息中的绝对位置，等同于本日志段对应文件的文件名
     */
    private long fileFromOffset;
    private File file;
    private MappedByteBuffer mappedByteBuffer;

    //使用ByteBuffer作为底层的容器，可以与内存映射文件对应起来，方便持久化，是mappedByteBuffer的副本
    private ByteBuffer byteBuffer;

    private ByteBuffer workingByteBuffer = ByteBuffer.allocate(MAX_MESSAGE_SIZE);

    public TQLog(final String fileName, final int fileSize) {
        init(fileName, fileSize);
    }

    private void init(final String fileName, final int fileSize){
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.file = new File(fileName);
        this.fileFromOffset = Long.parseLong(this.file.getName());
        boolean ok = false;

        try {
            this.fileChannel = new RandomAccessFile(this.file, "rw").getChannel();
            this.mappedByteBuffer = this.fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, fileSize);
            this.byteBuffer = this.mappedByteBuffer.slice();
            ok = true;
        } catch (FileNotFoundException e) {
            log.error("Failed to create file " + this.fileName, e);

        } catch (IOException e) {
            log.error("Failed to map file " + this.fileName, e);

        } finally {
            if (!ok && this.fileChannel != null) {
                try {
                    this.fileChannel.close();
                } catch (IOException e) {
                    log.error("Failed to close file " + this.fileName, e);
                }
            }
        }
    }

    /**
     * 插入单条消息
     * @param msg 被插入的消息
     * @return AppendMessageResult, 追加消息的结果
     */
    public AppendMessageResult appendMessageInner(final Message msg) {
        assert msg != null;

        int currentPos = this.wrotePosition.get();

        if (currentPos < this.fileSize) {
            this.byteBuffer.position(currentPos);
        }

        int msgLen = msg.calMsgLengthInByte();
        if (msgLen > MAX_MESSAGE_SIZE) {
            log.info("message size exceeded");
            return new AppendMessageResult(AppendMessageStatus.MESSAGE_SIZE_EXCEEDED,
                    -1, -1, -1);
        }


        if (currentPos + msgLen > this.fileSize) {
            log.info("this log has no more space, a new log is being created...");
            //将本文件状态置为不可读
            full.compareAndSet(false, true);

            /**
             * 告知调用者当前文件已满，需要新建一个文件，并告知其新建文件起始偏移量的绝对偏移量
             */
            return new AppendMessageResult(AppendMessageStatus.END_OF_FILE,
                    fileFromOffset + currentPos, -1, -1);
        }

        resetByteBuffer(this.workingByteBuffer, MAX_MESSAGE_SIZE);
        msg.serializeTo(this.workingByteBuffer);

        byteBuffer.put(workingByteBuffer.array(), 0, msgLen);

        AppendMessageResult result = new AppendMessageResult(AppendMessageStatus.PUT_OK,
                fileFromOffset + currentPos + msgLen, currentPos, msgLen);

        //增加本日志段内的偏移量到可写位置
        this.wrotePosition.getAndAdd(msgLen);

        return result;
    }

    /**
     * 从当前日志段中读取消息
     * @param readPos 读取位置
     * @param msgLen  消息大小
     * @return
     */
    public ByteBuffer getMessage(int readPos, int msgLen) {
        ByteBuffer byteBufferNew = null;
        if (readPos + msgLen <= wrotePosition.get()) {
            ByteBuffer byteBuffer =mappedByteBuffer.slice();
            byteBuffer.position(readPos);
            byteBufferNew= byteBuffer.slice();
            byteBufferNew.limit(msgLen);
        }
        return byteBufferNew;
    }


    /**
     * 重置ByteBuffer
     * @param byteBuffer 需要重置的ByteBuffer
     * @param limit 需要重置的大小，一般为Message.MAX_MESSAGE_SIZE
     */
    private void resetByteBuffer(final ByteBuffer byteBuffer, final int limit) {
        byteBuffer.flip();
        byteBuffer.limit(limit);
    }

    public void flush() {
        this.mappedByteBuffer.force();
    }

    public boolean isFull() {
        return full.get();
    }

    /**
     * 得到下一个写消息的绝对位置，以字节为单位
     * @return
     */
    public long getWritePos() {
        return fileFromOffset + this.wrotePosition.get();
    }
}
