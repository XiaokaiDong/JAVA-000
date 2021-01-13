package io.tt.mq.template.processor;

import java.util.function.BiConsumer;

public interface ConsumingProcessor<SESSION, DESTINATION> extends BiConsumer<SESSION, DESTINATION> {
}
