package top.iot.gateway.network.tcp.executor;

import top.iot.gateway.network.tcp.TcpMessage;
import top.iot.gateway.rule.engine.api.RuleData;
import top.iot.gateway.rule.engine.api.RuleDataCodec;
import top.iot.gateway.rule.engine.api.RuleDataCodecs;
import top.iot.gateway.rule.engine.executor.PayloadType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class TcpMessageCodec implements RuleDataCodec<TcpMessage> {

    private static final TcpMessageCodec instance = new TcpMessageCodec();

    static {
        RuleDataCodecs.register(TcpMessage.class, instance);
    }

    static void register() {
    }

    @Override
    public Object encode(TcpMessage data, Feature... features) {
        PayloadType payloadType = Feature.find(PayloadType.class, features)
                .orElse(PayloadType.BINARY);

        Map<String, Object> map = new HashMap<>();
        map.put("payload", payloadType.read(data.getPayload()));
        map.put("payloadType", payloadType.name());

        return map;
    }

    @Override
    public Flux<TcpMessage> decode(RuleData data, Feature... features) {
        return data
                .dataToMap()
                .flatMap(map -> {
                    Object payload = map.get("payload");
                    if (payload == null) {
                        return Mono.empty();
                    }
                    PayloadType payloadType = Feature
                            .find(PayloadType.class, features)
                            .orElse(PayloadType.BINARY);

                    TcpMessage message = new TcpMessage();
                    message.setPayload(payloadType.write(payload));
                    //message.setPayloadType(MessagePayloadType.valueOf(payloadType.name()));

                    return Mono.just(message);
                });
    }
}
