第14周作业——手工写一个消息队列

去掉了内存结构，采用文件作为消息的容器，使得消息可以持久化，可以堆积。

借鉴了RocketMQ的做法，但是大量使用了KAFKA的概念和术语。

目前（20210120）实现的功能有：

- 日志段(TQLog)——用文件保存消息。采用了将文件映射到内存的形式，这样可以避免内核到用户空间的内存拷贝。每个日志文件的大小暂时定为1GB。但无法保存更多的消息时，会自动新建下一个日志段文件。日志段文件的名称就是第一条消息在可无限扩张的日志文件中的起始偏移量。

  - 日志段可以保存消息。可以看出，消息是在文件内存映射MappedByteBuffer中进行的。

    ```java
    /**
     * 插入单条消息
     * @param msg 被插入的消息
     * @return
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
    ```

    上面返回的是一个AppendMessageResult，可以用于控制日志段文件的自动创建，自动创建文件参见下方对TQQueue#appendMessage的说明。
    ```java
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
    ```

  - 也可以查找消息（需要给出消息的位移和大小，这个可以很快的从消息索引文件中获取，参见下面对索引的介绍）
    ```java
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
    ```

- 日志文件(TQQueue)可以保存具体的消息，也可以保存消息的索引。当保存消息的时候，概念上，消息保存在一个无穷大的文件里，每次在这个无穷大文件末尾写入消息；实现上，这个无穷大消息文件是由多个日志段文件(TQLog)组成的.

  日志文件是真正对外提供读写接口的类，它调用和封装了日志段TQLog的读写接口

  - 追加消息。使用锁保证了线程安全，提高并发度应该靠提高主题的分区数目。追加消息时，会根据需要自动扩充日志段文件，并同时记录索引。追加消息调用了消息的序列化接口。

    ```java
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
    ```

  - 读取消息。依赖于索引文件快速定位消息的位置，消息的序列化由消息实现。消息本质上是二进制的，组成单元为”长度 + 内容“。

    ```java
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
    ```

- 目前定义了两种消息：具体的消息ConcreteMessage和索引消息MessageIndex，它们均实现了Message接口。

  - Message

  ```java
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
  ```

  - ConcreteMessage，就是通常意义上的业务消息，二进制格式，组成单元为”长度 + 内容“

    ```java
    /**
     * 简化自RocketMQ，最主要的，去除了事务相关的支持
    * 消息整体上采用定长格式，即
    *   4字节消息长度 +
    *   4字节版本号 +
    *   4字节队列ID +
    *   8字节队列偏移量 +
    *   8字节物理偏移量 +
    *   8字节消息ID +
    *   8字节创建时间戳 +
    *   4字节消息体长度头 +
    *   消息体 +
    *   1字节主题长度头 +
    *   主题 +
    *   2字节消息属性长度头 +
    *   消息属性
    */
    @Data
    @NoArgsConstructor
    public class ConcreteMessage implements Message
    ```

    - ConcreteMessage记录了消息ID，所属的分区、主题等信息，实现了序列化和反序列化
    ```java
    @Override
    public void serializeTo(ByteBuffer byteBuffer) {
        byte[] propertiesContent = ConcreteMessage.messageProperties2String(getProperties()).getBytes(Charset.forName("UTF-8"));
        byte[] topicContent = topic.getBytes(Charset.forName("UTF-8"));
        byte[] msgIdContent = msgId.getBytes(StandardCharsets.UTF_8);
        byteBuffer.putInt(calMsgLengthInByte());
        byteBuffer.putInt(ConcreteMessage.MESSAGE_MAGIC_NUM);
        byteBuffer.putInt(queueId);
        byteBuffer.putLong(fileOffset);
        byteBuffer.putLong(queueOffset);
        byteBuffer.putInt(msgIdContent.length);
        byteBuffer.put(msgIdContent);
        byteBuffer.putLong(bornTimestamp);
        byteBuffer.putInt(body.length);
        byteBuffer.put(body);
        byteBuffer.putInt(topicContent.length);
        byteBuffer.put(topicContent);
        byteBuffer.putShort((short)propertiesContent.length);
        byteBuffer.put(propertiesContent);
    }

    @Override
    public void deSerializeFrom(ByteBuffer byteBuffer) {
        int msgLen = byteBuffer.getInt();
        int msgVer = byteBuffer.getInt();
        queueId = byteBuffer.getInt();
        fileOffset = byteBuffer.getLong();
        queueOffset = byteBuffer.getLong();
        byte[] byteMsgId = new byte[byteBuffer.getInt()];  //跳过MSGID长度
        msgId = Charset.forName("UTF-8").decode(byteBuffer.get(byteMsgId, 0, byteMsgId.length)).toString();
        bornTimestamp = byteBuffer.getLong();
        int bodyLen = byteBuffer.getInt();
        body = new byte[bodyLen];
        body = byteBuffer.get(body, 0, bodyLen).array();
        int topicLen = byteBuffer.getInt();
        byte[] topicContent = new byte[topicLen];
        topic = Charset.forName("UTF-8").decode(byteBuffer.get(topicContent, 0, topicLen)).toString();
        short propertyLen = byteBuffer.getShort();
        byte[] propertyContent = new byte[propertyLen];
        String propertyString = Charset.forName("UTF-8").decode(byteBuffer.get(propertyContent, 0, propertyLen)).toString();
        properties = string2messageProperties(propertyString);
    }
    ```

  - MessageIndex，消息的索引，本身也存在日志段TQLog中，因为本身比较小，所以每个分区一个索引文件就足够了

    ```java
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
    ```

- 初步定义了Broker和topic

  - Broker。记录了”负责“的主题和消息分区
    ```java
    /**
     * 独立的数据节点
    */
    public class StandaloneBroker implements Broker {

        //本节点上的主题数
        private final AtomicInteger numOfTopics = new AtomicInteger(0);

        //本节点上的分区数，包含主副本和追随者副本。按照主题进行分类保存
        private Map<String, CopyOnWriteArraySet<TQQueue>> topicAndTQQueues = new ConcurrentHashMap<>();

        @Override
        public void start() {

        }

        @Override
        public void serve() {

        }

        @Override
        public void createQueue(String queueID, String topic) {
            CopyOnWriteArraySet<TQQueue> tqQueues = topicAndTQQueues.get(queueID);

            if (tqQueues == null) {
                tqQueues = new CopyOnWriteArraySet<TQQueue>();
                topicAndTQQueues.putIfAbsent(topic, tqQueues);
            }

            TQQueue tqQueue = new TQQueue(queueID, topic);
            tqQueues.add(tqQueue);
        }
    }

    ```
    
  - topic。Topic只是多个分区逻辑上的容器，做一些簿记工作。主题的元信息，应该只保留在ZooKeeper或类似的地方

    ```java
    /**
     * 主题，由若干个分区或者队列组成。
    * Topic只是多个分区逻辑上的容器，做一些簿记工作。主题的元信息，应该只保留在ZooKeeper或类似的地方
    */
    public class Topic {
        //主题的分区数
        private int numQueue;

        //主题的副本数
        private int replicaFactor;

        //本主题的分区或者队列，包括主副副本
        private final CopyOnWriteArrayList<String> partitions = new CopyOnWriteArrayList<>();


        //本主题的家目录，其所有的分区/队列TQQueue是这个目录下的子目录，子目录下是日志段文件TQLog
        private String basePath;

        //本主题所在的Broker列表
        private final CopyOnWriteArrayList<Broker> hostBrokers = new CopyOnWriteArrayList<>();

        public Topic(int numQueue, int replicaFactor, String basePath) {
            this.numQueue = numQueue;
            this.replicaFactor = replicaFactor;
            this.basePath = basePath;

        }
    }

    ```