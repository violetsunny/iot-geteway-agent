package top.iot.protocol.common.tcp;

import top.iot.protocol.common.tcp.message.*;
import top.iot.protocol.common.tcp.message.*;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import top.iot.gateway.core.Value;
import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.message.DeviceOnlineMessage;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.codec.*;
import top.iot.gateway.core.message.property.ReadPropertyMessage;
import top.iot.gateway.core.message.property.ReportPropertyMessage;
import top.iot.gateway.core.message.property.WritePropertyMessage;
import top.iot.gateway.core.server.session.DeviceSession;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@AllArgsConstructor
@Slf4j
public class DemoTcpMessageCodec implements DeviceMessageCodec {


    @Override
    public Transport getSupportTransport() {
        return DefaultTransport.TCP;
    }

    @Override
    @SneakyThrows
    public Mono<DeviceMessage> decode(MessageDecodeContext context) {
        log.debug("收到消息：");
        return Mono.defer(() -> {
            FromDeviceMessageContext ctx = ((FromDeviceMessageContext) context);
            byte[] payload =  context.getMessage().payloadAsBytes();
            if (log.isDebugEnabled()) {
                log.debug("handle tcp message:\n{}", Hex.encodeHexString(payload));
            }
            DemoTcpMessage message;
            try {
                message = DemoTcpMessage.of(payload);
                if (log.isDebugEnabled()) {
                    log.debug("decode tcp message:\n{}\n{}", Hex.encodeHexString(payload), message);
                }
            } catch (Exception e) {
                log.warn("decode tcp message error:[{}]", Hex.encodeHexString(payload), e);

                return Mono.error(e);
            }
            DeviceSession session = ctx.getSession();
            if (session.getOperator() == null) {
                //设备没有认证就发送了消息
                if (message.getType() != MessageType.AUTH_REQ) {
                    log.warn("tcp session[{}], unauthorized.", session.getId());
                    return session
                            .send(EncodedMessage.simple(DemoTcpMessage.of(MessageType.ERROR, ErrorMessage.of(TcpStatus.UN_AUTHORIZED)).toByteBuf()))
                            .then(Mono.fromRunnable(session::close));
                }
                AuthRequest request = ((AuthRequest) message.getData());
                String deviceId = buildDeviceId(request.getDeviceId());
                return context
                        .getDevice(buildDeviceId(request.getDeviceId()))
                        .flatMap(operator -> operator.getConfig("tcp_auth_key")
                                .map(Value::asString)
                                .filter(key -> Arrays.equals(request.getKey(), key.getBytes()))
                                .flatMap(msg -> {
                                    //认证通过
                                    DeviceOnlineMessage onlineMessage = new DeviceOnlineMessage();
                                    onlineMessage.setDeviceId(deviceId);
                                    onlineMessage.setTimestamp(System.currentTimeMillis());
                                    return session
                                            .send(EncodedMessage.simple(DemoTcpMessage.of(MessageType.AUTH_RES, AuthResponse.of(request.getDeviceId(), TcpStatus.SUCCESS)).toByteBuf()))
                                            .thenReturn(onlineMessage);
                                }))
                        //为空可能设备不存在或者没有配置tcp_auth_key,响应错误信息.
                        .switchIfEmpty(Mono.defer(() -> session
                                .send(EncodedMessage.simple(
                                        DemoTcpMessage.of(MessageType.AUTH_RES, AuthResponse.of(request.getDeviceId(), TcpStatus.ILLEGAL_ARGUMENTS)).toByteBuf()))
                                .then(Mono.empty())));
            }
            //keepalive, ping pong
            if (message.getType() == MessageType.PING) {
                return session
                        .send(EncodedMessage.simple(Unpooled.wrappedBuffer(DemoTcpMessage.of(MessageType.PONG, new Pong()).toBytes())))
                        .then(Mono.fromRunnable(session::ping));
            }
            if (message.getData() instanceof TcpDeviceMessage) {
                DeviceMessage tcpDeviceMessage = ((TcpDeviceMessage) message.getData()).toDeviceMessage();
                return session.send(EncodedMessage.simple(Unpooled.wrappedBuffer(tcpDeviceMessage.getDeviceId().getBytes())))
                        .thenReturn(tcpDeviceMessage);
            }
            return Mono.empty();
        });
    }

    public String buildDeviceId(long deviceId) {
        return String.valueOf(deviceId);
    }

    /**
     * demo tcp 报文协议格式
     * <p>
     * 第0字节为消息类型
     * 第1-4字节为消息体长度
     * 第5-n为消息体
     */
    @Override
    public Publisher<? extends EncodedMessage> encode(MessageEncodeContext context) {
        Message message = context.getMessage();
        EncodedMessage encodedMessage = null;
        log.info("推送设备消息，消息ID：{}", message.getMessageId());
        // 获取设备属性
        if (message instanceof ReadPropertyMessage) {
            ReadPropertyMessage readPropertyMessage = (ReadPropertyMessage) message;
            DemoTcpMessage of = DemoTcpMessage.of(MessageType.READ_PROPERTY, ReadProperty.of(readPropertyMessage));
            encodedMessage = EncodedMessage.simple(of.toByteBuf());
        }
        //修改设备属性
        if (message instanceof WritePropertyMessage) {
            WritePropertyMessage writePropertyMessage = (WritePropertyMessage) message;
            DemoTcpMessage of = DemoTcpMessage.of(MessageType.WRITE_PROPERTY, WriteProperty.of(writePropertyMessage));
            encodedMessage = EncodedMessage.simple(of.toByteBuf());
        }
        // 设备上报属性
        if (message instanceof ReportPropertyMessage) {
            ReportPropertyMessage reportPropertyMessage = (ReportPropertyMessage) message;
            DemoTcpMessage of = DemoTcpMessage.of(MessageType.REPORT_TEMPERATURE, ReportProperty.of(reportPropertyMessage));
            encodedMessage = EncodedMessage.simple(of.toByteBuf());
        }
        return encodedMessage != null ? Mono.just(encodedMessage) : Mono.empty();
    }
}
