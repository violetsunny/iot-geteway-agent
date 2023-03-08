package top.iot.protocol.common.http;

import com.alibaba.fastjson.JSONObject;
import top.iot.protocol.common.TopicMessageCodec;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.RepayableDeviceMessage;
import top.iot.gateway.core.message.codec.*;
import top.iot.gateway.core.message.codec.http.HttpExchangeMessage;
import top.iot.gateway.core.message.codec.http.SimpleHttpResponseMessage;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class HttpDeviceMessageCodec extends TopicMessageCodec implements DeviceMessageCodec {

    //WebClient webClient;

//    private DeviceBindManager bindManager;

    public Transport getSupportTransport() {
        return DefaultTransport.HTTP;
    }

    @Override
    public Mono<? extends Message> decode(MessageDecodeContext context) {

        return Mono.defer(() -> {
            HttpExchangeMessage message = (HttpExchangeMessage) context.getMessage();

            String topic = message.getUrl();
            JSONObject payload = message.payloadAsJson();

            return Mono.justOrEmpty(doDecode(null, topic, payload))
                .switchIfEmpty(Mono.defer(() -> {
                    //未转换成功，响应404
                    return message.response(SimpleHttpResponseMessage
                        .builder()
                        .status(404)
                        .contentType(MediaType.APPLICATION_JSON)
                        .payload(Unpooled.wrappedBuffer("{\"success\":false}".getBytes()))
                        .build()).then(Mono.empty());
                }))
                .flatMap(msg -> {
                    //响应成功
                    return message.response(SimpleHttpResponseMessage
                        .builder()
                        .status(200)
                        .contentType(MediaType.APPLICATION_JSON)
                        .payload(Unpooled.wrappedBuffer("{\"success\":true}".getBytes()))
                        .build())
                        .thenReturn(msg);
                });
        });
    }

    public Mono<EncodedMessage> encode(MessageEncodeContext context) {

        return context
            .reply(((RepayableDeviceMessage<?>) context.getMessage()).newReply().success())
            .then(Mono.empty());
        // 调用第三方接口
//       return webClient.post()
//            .uri("http://local-host.cn/")
//            .retrieve()
//            .bodyToMono(Map.class)
//            .map(map -> {
//                //处理返回值
//                DeviceMessage message = null;
//
//                return message;
//            })
//            .as(context::reply)
//            .then(Mono.empty());

        //return Mono.empty();

    }


}
