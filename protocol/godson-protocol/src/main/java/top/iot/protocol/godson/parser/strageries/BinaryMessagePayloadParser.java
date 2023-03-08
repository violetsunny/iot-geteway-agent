package top.iot.protocol.godson.parser.strageries;

import top.iot.protocol.godson.metadataMapping.DeviceMetadataMapping;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.extern.slf4j.Slf4j;
import top.iot.gateway.core.message.Headers;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.codec.EncodedMessage;
import top.iot.gateway.core.message.codec.MessagePayloadType;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
public class BinaryMessagePayloadParser extends StringMessagePayloadParser {

    @Override
    public MessagePayloadType getType() {
        return MessagePayloadType.BINARY;
    }

    @Override
    public Publisher<? extends Message> handle(String deviceId, DeviceMetadataMapping metadataMapping, EncodedMessage message) {
        log.info("binary解析器解码消息{}设备",deviceId);
        ByteBuf byteBuf = message.getPayload();
        byte[] bytes = ByteBufUtil.getBytes(byteBuf, 0, byteBuf.readableBytes(), false);

        List<Message> messages = handle(deviceId, metadataMapping, bytes);
        messages.forEach(msg -> {
            msg.addHeader(Headers.keepOnline,true); //设置让会话强制在线
//            message.addHeader(Headers.keepOnlineTimeoutSeconds,600);//设置超时时间（可选,默认10分钟），如果超过这个时间没有收到任何消息则认为离线。
        });
        log.info("binary解析器解析后的消息为：{}" , messages);
        return Flux.fromIterable(messages);
    }
}
