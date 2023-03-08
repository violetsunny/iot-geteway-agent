package top.iot.protocol.common.udp;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.message.codec.*;
import top.iot.gateway.core.server.session.DeviceSession;
import top.iot.protocol.common.tcp.MessageType;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * @author wangzheng
 * @see
 * @since 1.0
 */
@AllArgsConstructor
@Slf4j
public class DemoUdpMessageCodec implements DeviceMessageCodec {

    @Override
    public Transport getSupportTransport() {
        return DefaultTransport.UDP;
    }

    @Override
    public Flux<DeviceMessage> decode(MessageDecodeContext context) {
        return Flux.defer(() -> {
            FromDeviceMessageContext ctx = ((FromDeviceMessageContext) context);
            DeviceSession session=ctx.getSession();
            EncodedMessage encodedMessage = context.getMessage();
            JSONObject payload = JSON.parseObject(encodedMessage.getPayload().toString(StandardCharsets.UTF_8));
            return Mono
                    .justOrEmpty(top.iot.gateway.core.message.MessageType.<DeviceMessage>convertMessage(payload))
//                    .flatMapMany(msg->{
//                       return context
//                                .getDevice(msg.getDeviceId())
//                                .flatMapMany(operator -> operator.getConfig("udp_auth_key")
//                                        .map(Value::asString)
//                                        .filter(key -> key.equals(payload.getString("key")))
//                                        .flatMapMany(ignore -> {
//                                            //认证通过
//                                            return session
//                                                    .send(EncodedMessage.simple(Unpooled.wrappedBuffer("ok".getBytes())))
//                                                    .thenMany(Flux.just(msg));
//                                        }));
//
//                    }) .switchIfEmpty(Mono.defer(() -> session
//                            .send(EncodedMessage.simple(Unpooled.wrappedBuffer("ILLEGAL_ARGUMENTS".getBytes())))
//                            .then(Mono.empty())))
                ;


        });
    }

    @Override
    public Publisher<? extends EncodedMessage> encode(MessageEncodeContext context) {

        return Mono.just(EncodedMessage.simple(Unpooled.wrappedBuffer(context.getMessage().toString().getBytes())));
    }

    @Setter
    @Getter
    @AllArgsConstructor(staticName = "of")
    private static class Response {
        private MessageType type;

        private Object res;

    }

}
