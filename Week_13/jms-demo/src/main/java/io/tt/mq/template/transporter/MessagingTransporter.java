package io.tt.mq.template.transporter;

import io.tt.mq.template.processor.ConsumingProcessor;
import io.tt.mq.template.processor.ProducingProcessor;

import java.util.function.BiConsumer;

/**
 * 消息传递的泛型接口
 * @param <SESSION>  消息传递的上下文，一般位于底层的连接之上
 * @param <DESTINATION>  消息容器的名字
 */
public interface MessagingTransporter<SESSION, DESTINATION> {

    void close();
    void initConnectionFactory();

    SESSION getSession();

    /**
     * 处理消息，使用了回调模式
     * @param processor  处理器
     * @param session    会话
     * @param destination 消息的标的
     */
    default void processMessage(BiConsumer<SESSION, DESTINATION> processor,
                                SESSION session, DESTINATION destination){
        processor.accept(session, destination);
    }

    /**
     * 处理消息，使用了回调模式
     * @param producingProcessor  处理器
     * @param session    会话
     * @param destination 消息的标的
     */
    default void produceMessage(ProducingProcessor<SESSION, DESTINATION> producingProcessor,
                                SESSION session, DESTINATION destination) {
        processMessage(producingProcessor, session, destination);
    }

    /**
     * 处理消息，使用了回调模式
     * @param consumingProcessor  处理器
     * @param session    会话
     * @param destination 消息的标的
     */
    default void consumeMessage(ConsumingProcessor<SESSION, DESTINATION> consumingProcessor,
                                SESSION session, DESTINATION destination) {
        processMessage(consumingProcessor, session, destination);
    }
}
