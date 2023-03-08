package top.iot.protocol.godson.coap;

import top.iot.protocol.godson.TopicMessageCodec;
import top.iot.protocol.godson.metadataMapping.ProtocolConfigRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import top.iot.gateway.core.Value;
import top.iot.gateway.core.device.DeviceConfigKey;
import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.codec.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

/**
 * coap编解码处理器
 */
@AllArgsConstructor
@Slf4j
public class GodsonIotCoAPDeviceMessageCodec extends TopicMessageCodec implements DeviceMessageCodec {

    private final ProtocolConfigRegistry registry;

    public Transport getSupportTransport() {
        return DefaultTransport.CoAP;
    }

    @Override
    @Nonnull
    public Publisher<? extends Message> decode(@Nonnull MessageDecodeContext context) {
        log.info("上报coap消息解码处理");
        return Flux.just(context)
                .map(MessageDecodeContext::getMessage)
                .cast(CoapExchangeMessage.class)
                .flatMap(msg -> {
                    String topic = msg.getPath();
                    if (!topic.startsWith("/")) {
                        topic = "/".concat(topic);
                    }
                    String token = "1234";
                    if (topic.endsWith("/event") || topic.endsWith("/property")) {
                        return context.getDeviceAsync()
                                .flatMapMany(deviceOperator ->
                                        deviceOperator.getSelfConfig("key")
                                                .map(Value::asString)
                                                .filterWhen(key -> Mono.just(key.equals(token)))
                                                .flatMapMany(r -> deviceOperator.getAndRemoveConfig("will-msg")
                                                        .map(val -> val.as(DeviceMessage.class))
                                                        .doOnNext((msg2) ->
                                                                msg.getExchange().respond(msg2.toJson().toJSONString())
                                                        )
                                                )
                                                .flatMap(r -> registry.getMetadataMapping(deviceOperator.getSelfConfig(DeviceConfigKey.productId))
                                                        .flatMapMany(deviceMetadataMapping -> registry.getMessagePayloadParser(deviceMetadataMapping.getType())
                                                                . handle(deviceOperator.getDeviceId(), deviceMetadataMapping, msg))))
                                .switchIfEmpty(Mono.fromRunnable(() -> msg.getExchange().respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR, "token不正确")))
                                .doOnError(err -> msg.getExchange().respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR))
                                .doOnNext(res -> msg.getExchange().respond("success"));
                    }
                    //解码消息
                    String finalTopic = topic;
                    return context.getDeviceAsync()
                            .flatMap(deviceOperator ->
                                    deviceOperator.getSelfConfig("key")
                                            .switchIfEmpty(Mono.fromRunnable(() -> msg.getExchange().respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR, "token不正确")))
                                            .map(Value::asString)
                                            .filter(key -> key.equals(token))
                                            .flatMap(r -> deviceOperator.getAndRemoveConfig("will-msg")
                                                    .map(val -> val.as(DeviceMessage.class))
                                                    .doOnNext((msg2) ->
                                                            msg.getExchange().respond(msg2.toJson().toJSONString())
                                                    ).then(Mono
                                                            .justOrEmpty(doDecode(null, finalTopic, msg.payloadAsJson()))
                                                            .doOnSuccess(res -> {
                                                                if (res == null) {
                                                                    //响应成功消息
                                                                    msg.getExchange().respond("success");
                                                                } else {
                                                                    //响应4.04
                                                                    msg.getExchange().respond(CoAP.ResponseCode.NOT_FOUND);
                                                                }
                                                            })))
                                            .switchIfEmpty(Mono.empty())
                            );
                });
    }

    @Override
    @Nonnull
    public Mono<EncodedMessage> encode(@Nonnull MessageEncodeContext context) {
        return Mono.empty();
    }


}
