package top.iot.gateway.network.udp.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.iot.gateway.core.message.codec.EncodedMessage;

import java.nio.charset.StandardCharsets;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UdpMessage implements EncodedMessage {

    private String hostname;

    private int port;

    private ByteBuf payload;

    public String payloadAsString(){
        return getPayload().toString(StandardCharsets.UTF_8);
    }

    public UdpMessage(ByteBuf payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        ByteBufUtil.appendPrettyHexDump(builder,payload);

        return builder.toString();
    }
}