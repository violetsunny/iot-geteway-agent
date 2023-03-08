package top.iot.protocol.godson.tcp.message;

import top.iot.protocol.godson.tcp.TcpPayload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.iot.gateway.core.message.property.WritePropertyMessage;

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
}
