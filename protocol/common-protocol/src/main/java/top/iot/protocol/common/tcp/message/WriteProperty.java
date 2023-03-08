package top.iot.protocol.common.tcp.message;

import com.alibaba.fastjson.JSON;
import top.iot.protocol.common.tcp.TcpPayload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.iot.gateway.core.message.property.WritePropertyMessage;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class WriteProperty implements TcpPayload {
    private WritePropertyMessage writePropertyMessage;

    @Override
    public byte[] toBytes() {
        return writePropertyMessage.toString().getBytes();
    }

    @Override
    public void fromBytes(byte[] bytes, int offset) {
        WritePropertyMessage writePropertyMessage = new WritePropertyMessage();
        byte[] bytes1 = Arrays.copyOfRange(bytes, offset, bytes.length);
        String s = new String(bytes1, StandardCharsets.UTF_8);
        writePropertyMessage.fromJson(JSON.parseObject(s));
        this.writePropertyMessage = writePropertyMessage;
    }
}
