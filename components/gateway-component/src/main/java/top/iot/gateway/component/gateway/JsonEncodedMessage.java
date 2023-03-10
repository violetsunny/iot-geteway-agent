package top.iot.gateway.component.gateway;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import javax.annotation.Nonnull;
import java.util.Objects;

public class JsonEncodedMessage implements EncodableMessage {

    private volatile ByteBuf payload;

    @Getter
    private Object nativePayload;

    public JsonEncodedMessage(Object nativePayload) {
        Objects.requireNonNull(nativePayload);
        this.nativePayload = nativePayload;
    }

    @Nonnull
    @Override
    public ByteBuf getPayload() {
        if (payload == null) {
            //payload = PayloadType.JSON.write(nativePayload);
        }
        return payload;
    }


}
