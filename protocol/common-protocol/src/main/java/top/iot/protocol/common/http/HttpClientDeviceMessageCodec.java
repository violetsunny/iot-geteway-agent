package top.iot.protocol.common.http;

import lombok.AllArgsConstructor;
import top.iot.gateway.core.Value;
import top.iot.gateway.core.device.DeviceOperator;
import top.iot.gateway.core.device.DeviceRegistry;
import top.iot.gateway.core.device.DeviceState;
import top.iot.gateway.core.device.DeviceStateChecker;
import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.codec.*;
import top.iot.gateway.core.message.function.FunctionInvokeMessage;
import top.iot.gateway.core.server.session.DeviceSession;
import top.iot.gateway.core.server.session.DeviceSessionManager;
import org.reactivestreams.Publisher;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

@AllArgsConstructor
public class HttpClientDeviceMessageCodec implements DeviceMessageCodec, DeviceStateChecker {
    private final WebClient webClient;

    private final DeviceRegistry deviceRegistry;

    private final DeviceSessionManager sessionManager;

    public HttpClientDeviceMessageCodec(DeviceRegistry deviceRegistry, DeviceSessionManager sessionManager) {
        this(WebClient.create(), deviceRegistry, sessionManager);
    }

    @Override
    public Transport getSupportTransport() {
        return DefaultTransport.HTTP;
    }

    @Nonnull
    @Override
    public Publisher<? extends Message> decode(@Nonnull MessageDecodeContext messageDecodeContext) {
        return convertReply(messageDecodeContext.getMessage().payloadAsString());
    }

    @Nonnull
    @Override
    public Publisher<? extends EncodedMessage> encode(@Nonnull MessageEncodeContext messageEncodeContext) {
        DeviceMessage message = (DeviceMessage) messageEncodeContext.getMessage();
        DeviceOperator device = messageEncodeContext.getDevice();

        if (message instanceof FunctionInvokeMessage) {
            return createRequestBody(messageEncodeContext.getDevice(), message)
                .flatMapMany(body -> webClient
                    .post()
                    .uri("/{deviceId}/message", message.getDeviceId())
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(json -> convertReply(message, device, json)))
                .as(messageEncodeContext::reply) //直接回复结果
                .then(Mono.empty());
        }
        // TODO: 2020/10/12 其他消息处理

        return Mono.empty();
    }

    //构造消息请求
    protected Mono<DeviceMessage> createRequestBody(DeviceOperator device, DeviceMessage message) {

        // TODO: 2020/10/12
        return Mono.empty();
    }

    //转换消息响应
    protected Mono<DeviceMessage> convertReply(DeviceMessage from, DeviceOperator device, String json) {

        // TODO: 2020/10/12
        return Mono.empty();
    }

    //转换消息响应
    protected Mono<DeviceMessage> convertReply(String json) {

        // TODO: 2020/10/12


        return Mono.empty();
    }

    @Override
    public @NotNull Mono<Byte> checkState(@NotNull DeviceOperator device) {

        //获取设备状态
        return device
            .getConfigs("key")
            .flatMap(values ->
                webClient
                    .post()
                    .uri("/{deviceId}/state", device.getDeviceId())
                    //设置key
                    .header("key", values.getValue("key").map(Value::asString).orElse(null))
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(json -> {
                        // TODO: 2020/10/12 根据响应获取状态
                        byte state = DeviceState.online;
                        //session不存在则创建session
                        DeviceSession session = sessionManager.getSession(device.getDeviceId());
                        if (session == null) {
                            sessionManager.register(new HttpClientDeviceSession(device));
                        }
                        return state;
                    }));
    }
}
