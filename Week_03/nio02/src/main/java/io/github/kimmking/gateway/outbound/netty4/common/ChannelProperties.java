package io.github.kimmking.gateway.outbound.netty4.common;

import io.netty.util.AttributeKey;
import lombok.Data;

public enum ChannelProperties {

    SRC_URL("srcUrl");

    private AttributeKey<String> attributeKey;

    ChannelProperties(String propertyName){
        this.attributeKey = AttributeKey.valueOf(propertyName);
    }

    public AttributeKey<String> getAttributeKey() {
        return attributeKey;
    }

}
