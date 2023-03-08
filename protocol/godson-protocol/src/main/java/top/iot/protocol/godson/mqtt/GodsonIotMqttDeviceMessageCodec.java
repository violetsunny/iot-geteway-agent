package top.iot.protocol.godson.mqtt;

import com.alibaba.fastjson.JSON;
import top.iot.protocol.godson.TopicMessage;
import top.iot.protocol.godson.TopicMessageCodec;
import top.iot.protocol.godson.metadataMapping.ProtocolConfigRegistry;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.iot.gateway.core.device.DeviceConfigKey;
import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.message.DisconnectDeviceMessage;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.codec.*;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;

/**
 * mqtt编解码处理器
 */
@AllArgsConstructor
@Slf4j
public class GodsonIotMqttDeviceMessageCodec extends TopicMessageCodec implements DeviceMessageCodec {

    private final ProtocolConfigRegistry registry;

    /**
     * 协议类型:MQTT
     *
     */
    @Override
    public Transport getSupportTransport() {
        return DefaultTransport.MQTT;
    }

    @Override
    @Nonnull
    public Publisher<? extends Message> decode(@Nonnull MessageDecodeContext context) {
        log.info("上报mqtt消息解码处理");
        return
            Flux.just(context)
            .map(MessageDecodeContext::getMessage)
            .cast(MqttMessage.class)
            .flatMap(msg -> {
                String topic = msg.getTopic();
                if ("/message/webapi/gbycPollutant/report/property".equals(topic)) {
                    return context.getDeviceAsync().map(deviceOperator -> handleGBReportProperty(JSON.parseObject(msg.getPayload().toString(StandardCharsets.UTF_8))));
                    //return Mono.just(handleGBReportProperty(JSON.parseObject(msg.getPayload().toString(StandardCharsets.UTF_8))));
                }
                if ("/message/webapi/gbycPollutant/report/event".equals(topic)) {
                    return context.getDeviceAsync().flatMapMany(deviceOperator -> Flux.fromIterable(handleGBReportEvent(JSON.parseObject(msg.getPayload().toString(StandardCharsets.UTF_8)))));
                }
                if (topic.endsWith("/event") || topic.endsWith("/property")) {
                    return context.getDeviceAsync()
                        .flatMapMany(deviceOperator -> registry.getMetadataMapping(deviceOperator.getSelfConfig(DeviceConfigKey.productId))
                            .flatMapMany(deviceMetadataMapping -> registry.getMessagePayloadParser(deviceMetadataMapping.getType())
                                . handle(deviceOperator.getDeviceId(), deviceMetadataMapping, msg)));
                }
                return context.getDeviceAsync().map(deviceOperator -> doDecode(deviceOperator.getDeviceId(), topic, JSON.parseObject(msg.getPayload().toString(StandardCharsets.UTF_8))));
            });
    }

    @Override
    @Nonnull
    public Publisher<? extends EncodedMessage> encode(@Nonnull MessageEncodeContext context) {
        log.info("下发mqtt指令编码处理");
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

                return Mono.just(SimpleMqttMessage.builder()
                    .topic(msg.getTopic())
                    .payload(Unpooled.wrappedBuffer(JSON.toJSONBytes(msg.getMessage())))
                    .build());
            }
            return Mono.empty();
        });
    }

}