package top.iot.protocol.common.tcp.message;

import com.alibaba.fastjson.JSON;
import top.iot.protocol.common.tcp.TcpPayload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.iot.gateway.core.message.property.ReadPropertyMessage;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class ReadProperty implements TcpPayload {

    private ReadPropertyMessage readPropertyMessage;

    @Override
    public byte[] toBytes() {
        return readPropertyMessage.toString().getBytes();
    }

    @Override
    public void fromBytes(byte[] bytes, int offset) {
        ReadPropertyMessage readPropertyMessage = new ReadPropertyMessage();
        byte[] bytes1 = Arrays.copyOfRange(bytes, offset, bytes.length);
        String s = new String(bytes1, StandardCharsets.UTF_8);
        readPropertyMessage.fromJson(JSON.parseObject(s));
        this.readPropertyMessage = readPropertyMessage;
    }
}
