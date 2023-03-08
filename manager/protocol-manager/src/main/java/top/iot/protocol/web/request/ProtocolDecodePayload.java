package top.iot.protocol.web.request;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.web.bean.FastBeanCopier;
import top.iot.gateway.core.ProtocolSupport;
import top.iot.gateway.core.device.DeviceOperator;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.codec.*;
import top.iot.gateway.core.server.session.DeviceSession;
import top.iot.gateway.rule.engine.executor.PayloadType;
import org.reactivestreams.Publisher;

import javax.annotation.Nullable;

@Getter
@Setter
public class ProtocolDecodePayload {

    private DefaultTransport transport;

    private PayloadType payloadType = PayloadType.STRING;

    private String payload;

    public EncodedMessage toEncodedMessage() {
        if (transport == DefaultTransport.MQTT || transport == DefaultTransport.MQTT_TLS) {
            if (payload.startsWith("{")) {
                SimpleMqttMessage message = FastBeanCopier.copy(JSON.parseObject(payload), new SimpleMqttMessage());
                message.setPayloadType(MessagePayloadType.of(payloadType.getId()));
            }
            return SimpleMqttMessage.of(payload);
        } else if (transport == DefaultTransport.CoAP || transport == DefaultTransport.CoAP_DTLS) {
            return DefaultCoapMessage.of(payload);
        }
        return EncodedMessage.simple(payloadType.write(payload));
    }

    public Publisher<? extends Message> doDecode(ProtocolSupport support, DeviceOperator deviceOperator) {
        return support
            .getMessageCodec(getTransport())
            .flatMapMany(codec -> codec.decode(new FromDeviceMessageContext() {
                @Override
                public EncodedMessage getMessage() {
                    return toEncodedMessage();
                }

                @Override
                public DeviceSession getSession() {
                    return null;
                }

                @Nullable
                @Override
                public DeviceOperator getDevice() {
                    return deviceOperator;
                }
            }));
    }
}
