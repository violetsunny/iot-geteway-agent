package top.iot.protocol.godson.http;

import com.alibaba.fastjson.JSON;
import top.iot.protocol.godson.TopicMessageCodec;
import top.iot.protocol.godson.metadataMapping.ProtocolConfigRegistry;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.id.IDGenerator;
import top.iot.gateway.core.device.DeviceConfigKey;
import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.RepayableDeviceMessage;
import top.iot.gateway.core.message.codec.*;
import top.iot.gateway.core.message.codec.http.SimpleHttpRequestMessage;
import top.iot.gateway.core.message.function.FunctionInvokeMessage;
import org.reactivestreams.Publisher;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * http编解码处理器
 */
@AllArgsConstructor
@Slf4j
public class GodsonIotHttpDeviceMessageCodec extends TopicMessageCodec implements DeviceMessageCodec {

    private final ProtocolConfigRegistry registry;

//    WebClient webClient;

//    private DeviceBindManager bindManager;

    private static final WebClient webClient = WebClient.builder().build();

    public Transport getSupportTransport() {
        return DefaultTransport.HTTP;
    }

    @Override
    @Nonnull
    public Publisher<? extends Message> decode(@Nonnull MessageDecodeContext context) {
        log.info("上报http消息解码处理");
        return Flux.just(context)
                .map(MessageDecodeContext::getMessage)
                .cast(SimpleHttpRequestMessage.class)
                .flatMap(msg -> {
                    String topic = msg.getUrl();
                    String[] pathArr = msg.getPath().split("/");
                    String deviceId = pathArr[pathArr.length - 2];//deviceId在路径的倒数第二个
                    if (topic.endsWith("/event") || topic.endsWith("/property")) {
                        String payload = msg.getPayload().toString(CharsetUtil.UTF_8);
                        log.info("上报http消息 payload {}",payload);
                        Map<String, Object> payloadMap = new HashMap<>();
                        payloadMap.put("timestamp", System.currentTimeMillis());
                        payloadMap.put("eventId", IDGenerator.SNOW_FLAKE_STRING.generate());

                        if (topic.endsWith("/event")) {
                            payloadMap.put("events", payload);
                        } else {
                            payloadMap.put("properties", payload);
                        }
                        msg.setPayload(Unpooled.wrappedBuffer(JSON.toJSONString(payloadMap).getBytes()));
                        return context.getDevice(deviceId)
                                .switchIfEmpty(Mono.fromRunnable(() -> log.warn("device [{}] not fond in registry", deviceId)))
                                .flatMapMany(deviceOperator -> registry.getMetadataMapping(deviceOperator.getSelfConfig(DeviceConfigKey.productId))
                                        .switchIfEmpty(Mono.fromRunnable(() -> log.warn("product [{}] not fond in Metadata", deviceOperator.getSelfConfig(DeviceConfigKey.productId))))
                                        .flatMapMany(deviceMetadataMapping -> registry.getMessagePayloadParser(deviceMetadataMapping.getType())
                                                .handle(deviceOperator.getDeviceId(), deviceMetadataMapping, msg)));
                    }
                    return context.getDevice(deviceId)
                            .switchIfEmpty(Mono.fromRunnable(() -> log.warn("device [{}] not fond in registry", deviceId)))
                            .map(deviceOperator -> doDecode(deviceOperator.getDeviceId(), topic, msg.payloadAsJson()));
                });
    }

    @Override
    @Nonnull
    public Mono<EncodedMessage> encode(@Nonnull MessageEncodeContext context) {


//        return context
//            .reply(((RepayableDeviceMessage<?>) context.getMessage()).newReply().success())
//            .then(Mono.empty());
        // 调用第三方接口
//        SimpleHttpRequestMessage message = new SimpleHttpRequestMessage();
//        DeviceMessage msg = (DeviceMessage) context.getMessage();
//
//        if (msg instanceof FunctionInvokeMessage) {
//            Map<String, Object> map = ((FunctionInvokeMessage) msg).inputsToMap();

//            ;
//            message.setQueryParameters(FastBeanCopier.copy(map, new HashMap<String, String>()));
////            message.setPayload(Unpooled.wrappedBuffer(JSONObject.toJSONString(data).getBytes()));
//        }
//
//        message.setContentType(MediaType.APPLICATION_JSON);
//        message.setMethod(HttpMethod.GET);
//        message.setPath("/api/jl/remoteOpenLock");
//        return context
//            .reply(((RepayableDeviceMessage<?>) context.getMessage()).newReply().success()).then(Mono.just(message));


        DeviceMessage message = (DeviceMessage) context.getMessage();
        Map<String, Object> paramMap = new HashMap<>();
        if (message instanceof FunctionInvokeMessage) {
            paramMap.putAll(((FunctionInvokeMessage) message).inputsToMap());
        }

        return webClient.get()
                .uri("http://www.bzccspt.com/api/jl/remoteOpenLock?lockCode={lockCode}&password={password}&validTime={validTime}", paramMap)
                .retrieve()
                .bodyToMono(Map.class)
                .map(map -> {
                    //处理返回值
                    return ((RepayableDeviceMessage<?>) context.getMessage()).newReply().success();
                })
                .as(context::reply)
                .then(Mono.empty());


    }


}