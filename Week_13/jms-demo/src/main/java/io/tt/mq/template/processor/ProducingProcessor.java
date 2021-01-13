package io.tt.mq.template.processor;

import java.util.function.BiConsumer;

public interface ProducingProcessor <SESSION, DESTINATION> extends BiConsumer<SESSION, DESTINATION> {
}
