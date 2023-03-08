package top.iot.protocol.common.tcp.message;

import top.iot.protocol.common.tcp.TcpPayload;
import top.iot.protocol.common.tcp.TcpStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class ErrorMessage implements TcpPayload {

    TcpStatus status;

    @Override
    public byte[] toBytes() {
        return new byte[]{status.getStatus()};
    }

    @Override
    public void fromBytes(byte[] bytes, int offset) {
        status = TcpStatus.of(bytes[offset]).orElse(TcpStatus.UNKNOWN);
    }
}
