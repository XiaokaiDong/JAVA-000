package io.tt.tmq.core.message.impl;

import io.tt.tmq.core.message.Message;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

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
public class ConcreteMessage implements Message {
    private static final long serialVersionUID = 8445773977080406428L;

    public static final String PROPERTY_WAIT_STORE_MSG_OK = "WAIT";

    //用于消息属性序列化
    public static final char NAME_VALUE_SEPARATOR = 1;
    public static final char PROPERTY_SEPARATOR = 2;

    //最大的消息大小。
    public static final int MAX_MESSAGE_SIZE = 10 * 1024 * 1024;

    //基本属性
    private String topic;
    //private int flag;
    private Map<String, String> properties;
    private byte[] body;

    //和队列相关的属性
    private String brokerName;
    //队列ID
    private int queueId;
    private int storeSize;
    //队列中的偏移量，即绝对偏移量或者物理偏移量
    private long queueOffset;
    //本日志段内的偏移量
    private long fileOffset;

    //自身属性
    //生成时间戳
    private long bornTimestamp;
    //消息ID
    private String msgId;
    //消息版本号，写代码是的日期
    public final static int MESSAGE_MAGIC_NUM = 20210119;

    public ConcreteMessage() {
    }


    public ConcreteMessage(String topic, byte[] body, boolean waitStoreMsgOK) {
        this.topic = topic;
        //this.flag = flag;
        this.body = body;

        this.setWaitStoreMsgOK(waitStoreMsgOK);
    }

    public ConcreteMessage(String topic, byte[] body) {
        this(topic, body, true);
    }

    void putProperty(final String name, final String value) {
        if (null == this.properties) {
            this.properties = new HashMap<String, String>();
        }

        this.properties.put(name, value);
    }

    void clearProperty(final String name) {
        if (null != this.properties) {
            this.properties.remove(name);
        }
    }

    public String getProperty(final String name) {
        if (null == this.properties) {
            this.properties = new HashMap<String, String>();
        }

        return this.properties.get(name);
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public boolean isWaitStoreMsgOK() {
        String result = this.getProperty(PROPERTY_WAIT_STORE_MSG_OK);
        if (null == result)
            return true;

        return Boolean.parseBoolean(result);
    }

    public void setWaitStoreMsgOK(boolean waitStoreMsgOK) {
        this.putProperty(PROPERTY_WAIT_STORE_MSG_OK, Boolean.toString(waitStoreMsgOK));
    }


    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * 计算消息的大小
     * @return 以字节为单位的消息大小。当大小超过最大大小MAX_MESSAGE_SIZE时返回-1
     */
    public int calMsgLengthInByte() {
        byte[] propertiesContent = messageProperties2String(this.properties).getBytes(Charset.forName("UTF-8"));
        final int msgLen = 4 //整体长度
                + 4 //版本号
                + 4 //队列ID
                + 8 //队列内偏移量
                + 8 //物理偏移量
                + 4 //消息ID长度
                + 8 //消息ID
                + 8 //创建时间戳
                + 4 //消息体长度头
                + (body == null ? 0 : body.length) //消息体
                + 1 //位主题长度头
                + topic.getBytes(Charset.forName("UTF-8")).length //主题
                + 2 //消息属性长度头
                + propertiesContent.length;//消息属性

        return msgLen > MAX_MESSAGE_SIZE ? -1 : msgLen;
    }

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

    public static String messageProperties2String(Map<String, String> properties) {
        StringBuilder sb = new StringBuilder();
        if (properties != null) {
            for (final Map.Entry<String, String> entry : properties.entrySet()) {
                final String name = entry.getKey();
                final String value = entry.getValue();

                if (value == null) {
                    continue;
                }
                sb.append(name);
                sb.append(NAME_VALUE_SEPARATOR);
                sb.append(value);
                sb.append(PROPERTY_SEPARATOR);
            }
        }
        return sb.toString();
    }

    public static Map<String, String> string2messageProperties(final String properties) {
        Map<String, String> map = new HashMap<String, String>();
        if (properties != null) {
            String[] items = properties.split(String.valueOf(PROPERTY_SEPARATOR));
            for (String i : items) {
                String[] nv = i.split(String.valueOf(NAME_VALUE_SEPARATOR));
                if (2 == nv.length) {
                    map.put(nv[0], nv[1]);
                }
            }
        }

        return map;
    }
}