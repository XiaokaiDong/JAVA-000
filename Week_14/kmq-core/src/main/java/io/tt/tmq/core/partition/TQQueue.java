package io.tt.tmq.core.partition;

import io.tt.tmq.core.message.impl.ConcreteMessage;
import io.tt.tmq.core.message.impl.MessageIndex;
import lombok.Data;

import java.nio.ByteBuffer;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 存放消息的队列，类似于KAFKA的分区或者ROCKETMQ的队列，由多个TQLog日志段组成
 * 线程安全
 */
@Data
public class TQQueue {
    //存放所有的日志段TQLog
    private final CopyOnWriteArrayList<TQLog> tqLogs = new CopyOnWriteArrayList<>();

    //本分区中所有消息的序列号，注意不是文件中的偏移！
    private final AtomicLong maxMsgSeqNo = new AtomicLong(0);

    //领导者副本还是追随者副本，初始都是追随者，经过选主后，才会有领导者副本
    private boolean beLeader = false;

    /**
     * 本分区所使用的索引文件
     */
    private TQLog indexFile;

    private MessageIndex indexItem;

    //本分区所属的主题名
    private String topic;

    //本分区的ID
    private String queueId;

    public TQLog getLastTQLog() {
        return tqLogs.get(tqLogs.size() - 1);
    }

    public TQQueue(String queueId, String topic) {
        this.queueId = queueId;
        this.topic = topic;
        indexFile = new TQLog(queueId + "idx", 1 * 1024 * 1024 * 1024);
    }

    public AppendMessageResult appendMessage(ConcreteMessage msg) {

        TQLog tqLog = tqLogs.get(tqLogs.size() - 1);
        if (tqLog.isFull()) {
            //创建一个新的日志段
            tqLog = new TQLog(String.format("%20d", tqLog.getWritePos()), TQLog.MAX_LOG_SIZE);
            tqLogs.add(tqLog);
        }

        AppendMessageResult result = null;

        /**
         * 用锁来保证线程安全，tmq只会有顺序写，且只有这一个地方涉及到写操作。
         * 增加并发度依靠多分区来实现。
         */
        synchronized (this) {

            result = tqLog.appendMessageInner(msg);
            if (result.getStatus() == AppendMessageStatus.END_OF_FILE) {
                //当前日志段无法存放消息，再新建一个日志段，起始位置从result中获取
                tqLog = new TQLog(String.format("%20d", result.getAbsPosition()), TQLog.MAX_LOG_SIZE);
                tqLogs.add(tqLog);

                result = tqLog.appendMessageInner(msg);
            }

            //消息序号加一
            maxMsgSeqNo.getAndIncrement();

            indexItem.setNumTQLog(tqLogs.size() - 1);
            indexItem.setAbsPos(result.getAbsPosition());
            indexItem.setPosInTQLog((int)result.getPosInCurrentLog());

            indexFile.appendMessageInner(indexItem);
        }

        return result;
    }

    public ConcreteMessage getMessage(long msgNo) {
        ConcreteMessage result = new ConcreteMessage();
        if (msgNo <= maxMsgSeqNo.get()){
            //先查询索引文件
            long readPos = MessageIndex.MESSAGE_INDEX_SIZE * msgNo;
            ByteBuffer indexBuffer = indexFile.getMessage((int)readPos, MessageIndex.MESSAGE_INDEX_SIZE);
            MessageIndex messageIndex = new MessageIndex();
            messageIndex.deSerializeFrom(indexBuffer);

            //得到消息本身
            TQLog tqLog = tqLogs.get(messageIndex.getNumTQLog());
            ByteBuffer byteBuffer = tqLog.getMessage(messageIndex.getPosInTQLog(), messageIndex.getMsgLen());

            result.deSerializeFrom(byteBuffer);
        }

        return result;
    }

}
