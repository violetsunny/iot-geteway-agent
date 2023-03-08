package top.iot.gateway.component.gateway;

import top.iot.gateway.core.message.codec.EncodedMessage;

public interface EncodableMessage extends EncodedMessage {

    Object getNativePayload();

    static EncodableMessage of(Object object) {
        return new JsonEncodedMessage(object);
    }
}
