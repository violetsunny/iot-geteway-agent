package top.iot.protocol.godson.tcp.message;

import top.iot.protocol.godson.tcp.TcpPayload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.iot.gateway.core.message.property.ReadPropertyMessage;

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
}
