package top.iot.gateway.component.gateway;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import top.iot.gateway.network.NetworkType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.codec.EncodedMessage;
import top.iot.gateway.core.message.codec.MessagePayloadType;
import top.iot.gateway.core.message.codec.Transport;
import top.iot.gateway.core.utils.BytesUtils;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

/**
 * 设备网关,用于统一管理设备连接,状态以及消息收发
 *
 * @author zhouhao
 * @version 1.0
 * @since 1.0
 */
public interface DeviceGateway {

    /**
     * @return 网关ID
     */
    String getId();

    /**
     * @return 传输协议
     * @see top.iot.gateway.core.message.codec.DefaultTransport
     */
    Transport getTransport();

    /**
     * @return 网络类型
     * @see NetworkType
     */
    NetworkType getNetworkType();

    /**
     * 订阅来自设备到消息,关闭网关时不会结束流.
     *
     * @return 设备消息流
     */
    Flux<Message> onMessage();

    /**
     * 启动网关
     *
     * @return 启动结果
     */
    Mono<Void> startup();

    /**
     * 暂停网关,暂停后停止处理设备消息.
     *
     * @return 暂停结果
     */
    Mono<Void> pause();

    /**
     * 关闭网关
     *
     * @return 关闭结果
     */
    Mono<Void> shutdown();

    default boolean isAlive() {
        return true;
    }

    default boolean isStarted() {
        return getState() == GatewayState.started;
    }

    default GatewayState getState() {
        return GatewayState.started;
    }

    default void doOnStateChange(BiConsumer<GatewayState, GatewayState> listener) {

    }

    default void doOnShutdown(Disposable disposable) {
        doOnStateChange((before, after) -> {
            if (after == GatewayState.shutdown) {
                disposable.dispose();
            }
        });
    }

    @SneakyThrows
    default String getDeviceId(EncodedMessage message, String deviceCode) {
        String deviceId = null;

        if (message.getPayloadType() != MessagePayloadType.BINARY) {
            String payload = message.getPayload().toString(StandardCharsets.UTF_8);
            String base64Pattern = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{4}|[A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)$";
            if (Pattern.matches(base64Pattern, payload)) {
                payload = new String(Base64.getDecoder().decode(payload));
            }

            if (message.getPayloadType() == MessagePayloadType.JSON) {
                String deviceCodeName = "deviceId";
                if (deviceCode != null && !"{}".equals(deviceCode)) {
                    deviceCodeName = Optional.ofNullable(JSON.parseObject(deviceCode).getString(message.getPayloadType().name().toLowerCase())).orElse("deviceId");
                }
                deviceId = JSONObject.parseObject(payload).getString(deviceCodeName);
            } else {
                if (StringUtils.hasText(deviceCode)) {
                    String deviceCodeName = Optional.ofNullable(JSON.parseObject(deviceCode).getString(message.getPayloadType().name().toLowerCase())).orElseThrow(() -> new RuntimeException("deviceCode未设置"));
                    deviceId = String.valueOf(BytesUtils.beToInt(payload.getBytes(), Integer.parseInt(deviceCodeName.split(",")[0]), Integer.parseInt(deviceCodeName.split(",")[1])));
                }
            }
        } else {
            if (StringUtils.hasText(deviceCode)) {
                String deviceCodeName = Optional.ofNullable(JSON.parseObject(deviceCode).getString(message.getPayloadType().name().toLowerCase())).orElseThrow(() -> new RuntimeException("deviceCode未设置"));
                String[] deviceCodeArr = deviceCodeName.split(",");
                if (deviceCodeArr.length == 2) {
                    deviceId = String.valueOf(BytesUtils.beToInt(ByteBufUtil.getBytes(message.getPayload(), 0, message.getPayload().readableBytes(), false), Integer.parseInt(deviceCodeArr[0]), Integer.parseInt(deviceCodeArr[1])));
                } else if (deviceCodeArr.length>2){
                    ByteBuf byteBuf = message.getPayload();
                    byte[] bytes = ByteBufUtil.getBytes(byteBuf, 0, byteBuf.readableBytes(), false);
                    if ("hex".equals(deviceCodeArr[2])) {
                        deviceId = String.valueOf(BytesUtils.leToLong(bytes, Integer.parseInt(deviceCodeArr[0]), Integer.parseInt(deviceCodeArr[1])));
                    } else {
                        //bcd
                        deviceId = Hex.encodeHexString(bytes).substring(2*Integer.parseInt(deviceCodeArr[0]), 2*(Integer.parseInt(deviceCodeArr[0])+Integer.parseInt(deviceCodeArr[1])));
                        if ("hex-float".equals(deviceCodeArr[2])) {
                            deviceId = new String(Hex.decodeHex(deviceId));
                        }
                    }
                }
            }
        }
        return deviceId;
    }
}
