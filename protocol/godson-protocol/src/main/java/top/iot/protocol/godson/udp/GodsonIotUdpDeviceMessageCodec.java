package top.iot.protocol.godson.udp;

import top.iot.protocol.godson.metadataMapping.ProtocolConfigRegistry;
import top.iot.protocol.godson.tcp.TcpMessageType;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import top.iot.gateway.core.Value;
import top.iot.gateway.core.device.DeviceConfigKey;
import top.iot.gateway.core.message.DeviceOnlineMessage;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.codec.*;
import top.iot.gateway.core.server.session.DeviceSession;
import org.reactivestreams.Publisher;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Map;

/**
 * @author wangzheng
 * @see
 * @since 1.0
 */
@AllArgsConstructor
@Slf4j
public class GodsonIotUdpDeviceMessageCodec implements DeviceMessageCodec {

    private final ProtocolConfigRegistry registry;

    @Override
    public Transport getSupportTransport() {
        return DefaultTransport.UDP;
    }

    @Override
    @Nonnull
    public Flux<? extends Message> decode(@Nonnull MessageDecodeContext context) {
        log.debug("收到udp消息：");

        FromDeviceMessageContext ctx = ((FromDeviceMessageContext) context);
        byte[] payload =  context.getMessage().payloadAsBytes();
        String hexString = Hex.encodeHexString(payload);
        if (log.isDebugEnabled()) {
            log.debug("handle udp message:\n{}", hexString);
        }

        DeviceSession session = ctx.getSession();

        return Flux.just(context)
            .map(MessageDecodeContext::getMessage)
            .flatMap(msg -> context.getDeviceAsync()
                .flatMapMany(deviceOperator -> registry.getMetadataMapping(deviceOperator.getSelfConfig(DeviceConfigKey.productId))
                    .flatMapMany(deviceMetadataMapping -> {
                        String msgType = deviceMetadataMapping.getMsgType().entrySet().stream().filter(entry -> {
                            boolean flag = hexString.startsWith(String.valueOf(entry.getValue()));
                            if (deviceMetadataMapping.getMsgInfo("lowLenLimit").isPresent()) {
                                flag = flag & hexString.length() >= (int)deviceMetadataMapping.getMsgInfo("lowLenLimit").get();
                            }
                            return flag;
                        }).map(Map.Entry::getKey).findFirst().orElse(null);
                        if (!StringUtils.hasText(msgType)) return Mono.empty();

                        if (msgType.equals(TcpMessageType.AUTH_REQ.name())) {
                            Object authKeyObj = deviceMetadataMapping.getMsgInfo("authKey").orElse(null);
                            if (authKeyObj == null) return Mono.empty();
                            String[] authKeyArr = String.valueOf(authKeyObj).split(",");
                            if (authKeyArr.length != 2) return Mono.empty();
                            byte[] authKey = Arrays.copyOfRange(payload, Integer.parseInt(authKeyArr[0]), Integer.parseInt(authKeyArr[1]));

                            return deviceOperator.getConfig("udp_auth_key")
                                .map(Value::asString)
                                .filter(key -> Arrays.equals(authKey, key.getBytes()))
                                .flatMapMany(r -> {
                                    //认证通过
                                    DeviceOnlineMessage onlineMessage = new DeviceOnlineMessage();
                                    onlineMessage.setDeviceId(session.getDeviceId());
                                    onlineMessage.setTimestamp(System.currentTimeMillis());
                                    Object authRes = deviceMetadataMapping.getMsgType("AUTH_RES").orElse(null);
                                    if (authRes != null) {
                                        try {
                                            return session
                                                .send(EncodedMessage.simple(Unpooled.wrappedBuffer(Hex.decodeHex(String.valueOf(authRes)))))
                                                .thenReturn(onlineMessage);
                                        } catch (DecoderException e) {
                                            log.error("响应AUTH_RES十六进制字符串{}转字节数组失败", authRes);
                                        }

                                    }
                                    return Mono.just(onlineMessage);
                                })
                                //为空可能设备不存在或者没有配置tcp_auth_key,响应错误信息.
                                .switchIfEmpty(Mono.empty());
                        }
                        return registry.getMessagePayloadParser(deviceMetadataMapping.getType())
                            . handle(deviceOperator.getDeviceId(), deviceMetadataMapping, msg);
                    }))
            );
    }

    @Override
    @Nonnull
    public Publisher<? extends EncodedMessage> encode(@Nonnull MessageEncodeContext context) {

        return Mono.just(EncodedMessage.simple(Unpooled.wrappedBuffer(context.getMessage().toString().getBytes())));
    }

    @Setter
    @Getter
    @AllArgsConstructor(staticName = "of")
    private static class Response {
        private TcpMessageType type;

        private Object res;

    }

}
