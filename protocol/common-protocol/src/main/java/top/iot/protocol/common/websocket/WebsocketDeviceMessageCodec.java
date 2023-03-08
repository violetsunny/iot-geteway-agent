package top.iot.protocol.common.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.Unpooled;
import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.message.DisconnectDeviceMessage;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.codec.*;
import top.iot.gateway.core.message.codec.http.websocket.DefaultWebSocketMessage;
import top.iot.gateway.core.message.codec.http.websocket.WebSocketMessage;
import top.iot.gateway.core.message.codec.http.websocket.WebSocketSession;
import top.iot.gateway.core.message.codec.http.websocket.WebSocketSessionMessage;
import top.iot.protocol.common.TopicMessage;
import top.iot.protocol.common.TopicMessageCodec;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;


public class WebsocketDeviceMessageCodec extends TopicMessageCodec implements DeviceMessageCodec {

    public Transport getSupportTransport() {
        return DefaultTransport.WebSocket;
    }

    @Override
    public Mono<? extends Message> decode(MessageDecodeContext context) {

        return Mono.defer(() -> {
            WebSocketSessionMessage mqttMessage = (WebSocketSessionMessage) context.getMessage();
            WebSocketSession session = mqttMessage.getWebSocketSession();

            JSONObject payload = JSON.parseObject(mqttMessage.getPayload().toString(StandardCharsets.UTF_8));

            return Mono.justOrEmpty(doDecode(null, session.getUri(), payload))
                .switchIfEmpty(Mono.defer(() -> {
                    //未转换成功，响应404
                    return session
                        .send(session.textMessage("{\"status\":404}"))
                        .then(Mono.empty());
                }));
        });
    }

    public Mono<EncodedMessage> encode(MessageEncodeContext context) {
        Message message = context.getMessage();
        return Mono.defer(() -> {
            if (message instanceof DeviceMessage) {
                if (message instanceof DisconnectDeviceMessage) {
                    return ((ToDeviceMessageContext) context)
                        .disconnect()
                        .then(Mono.empty());
                }

                TopicMessage msg = doEncode((DeviceMessage) message);
                if (null == msg) {
                    return Mono.empty();
                }
                JSONObject data = new JSONObject();
                data.put("topic", msg.getTopic());
                data.put("message", msg.getMessage());

                return Mono.just(DefaultWebSocketMessage.of(WebSocketMessage.Type.TEXT, Unpooled.wrappedBuffer(data.toJSONString().getBytes())));
            }
            return Mono.empty();

        });
    }


}
