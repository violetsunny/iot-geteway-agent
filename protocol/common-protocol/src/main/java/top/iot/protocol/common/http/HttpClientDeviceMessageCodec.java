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
                .as(messageEncodeContext::reply) //??????????????????
                .then(Mono.empty());
        }
        // TODO: 2020/10/12 ??????????????????

        return Mono.empty();
    }

    //??????????????????
    protected Mono<DeviceMessage> createRequestBody(DeviceOperator device, DeviceMessage message) {

        // TODO: 2020/10/12
        return Mono.empty();
    }

    //??????????????????
    protected Mono<DeviceMessage> convertReply(DeviceMessage from, DeviceOperator device, String json) {

        // TODO: 2020/10/12
        return Mono.empty();
    }

    //??????????????????
    protected Mono<DeviceMessage> convertReply(String json) {

        // TODO: 2020/10/12


        return Mono.empty();
    }

    @Override
    public @NotNull Mono<Byte> checkState(@NotNull DeviceOperator device) {

        //??????????????????
        return device
            .getConfigs("key")
            .flatMap(values ->
                webClient
                    .post()
                    .uri("/{deviceId}/state", device.getDeviceId())
                    //??????key
                    .header("key", values.getValue("key").map(Value::asString).orElse(null))
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(json -> {
                        // TODO: 2020/10/12 ????????????????????????
                        byte state = DeviceState.online;
                        //session??????????????????session
                        DeviceSession session = sessionManager.getSession(device.getDeviceId());
                        if (session == null) {
                            sessionManager.register(new HttpClientDeviceSession(device));
                        }
                        return state;
                    }));
    }
}
