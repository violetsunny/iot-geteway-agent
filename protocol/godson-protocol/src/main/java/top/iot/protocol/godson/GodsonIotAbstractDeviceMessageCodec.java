package top.iot.protocol.godson;//package org.godson.core.protocol;
//
//import lombok.extern.slf4j.Slf4j;
//import top.iot.gateway.core.message.Message;
//import top.iot.gateway.core.message.codec.*;
//import org.reactivestreams.Publisher;
//import reactor.core.publisher.Flux;
//
//import javax.annotation.Nonnull;
//
//@Slf4j
//public abstract class GodsonIotAbstractDeviceMessageCodec implements DeviceMessageCodec {
//
//    /**
//     * 协议类型:MQTT/TCP/HTTP
//     *
//     * @return
//     */
//    @Override
//    public abstract Transport getSupportTransport();
//
//
//    /**
//     * 上报消息解码处理
//     *
//     * @param context
//     * @return
//     */
//    @Nonnull
//    @Override
//    public Publisher<? extends Message> decode(@Nonnull MessageDecodeContext context) {
//        log.info("上报消息解码处理");
//        return Flux.just(context)
//             .map(MessageDecodeContext::getMessage)
//             .flatMap(msg -> context.getDeviceAsync()
//                 .flatMapMany(deviceOperator -> deviceOperator.getMetadataMapping()
//                    .flatMapMany(deviceMetadataMapping -> context.getMessagePayloadParserBuilder()
//                        .build(msg.getPayloadType())
//                        . handle(deviceOperator.getDeviceId(), deviceMetadataMapping, msg)))
//             );
//    }
//
//    @Nonnull
//    @Override
//    public Publisher<? extends EncodedMessage> encode(@Nonnull MessageEncodeContext context) {
//        return null;
//    }
//
//}
