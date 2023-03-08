package top.iot.protocol.web.request;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import top.iot.gateway.core.ProtocolSupport;
import top.iot.gateway.core.device.DeviceOperator;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.MessageType;
import top.iot.gateway.core.message.codec.DefaultTransport;
import top.iot.gateway.core.message.codec.MessageEncodeContext;
import top.iot.gateway.core.message.codec.MqttMessage;
import top.iot.gateway.rule.engine.executor.PayloadType;
import org.reactivestreams.Publisher;

import javax.annotation.Nullable;

@Getter
@Setter
public class ProtocolEncodePayload {

    private DefaultTransport transport;

    private String payload;

    private PayloadType payloadType = PayloadType.STRING;

    public Message toDeviceMessage() {
        return MessageType.convertMessage(JSON.parseObject(payload))
            .orElseThrow(() -> new IllegalArgumentException("无法识别的消息"));
    }

    public Publisher<Object> doEncode(ProtocolSupport support, DeviceOperator operator) {
        return support.getMessageCodec(getTransport())
            .flatMapMany(codec -> codec.encode(new MessageEncodeContext() {
                @Override
                public Message getMessage() {
                    return toDeviceMessage();
                }

                @Nullable
                @Override
                public DeviceOperator getDevice() {
                    return operator;
                }
            }))
            .map(msg -> {
                if (msg instanceof MqttMessage) {
                    JSONObject obj = (JSONObject) JSON.toJSON(msg);
                    obj.put("payload", payloadType.read(msg.getPayload()));
                    obj.remove("bytes");
                    return obj;
                }
                return getPayloadType().read(msg.getPayload());
            });
    }
}
