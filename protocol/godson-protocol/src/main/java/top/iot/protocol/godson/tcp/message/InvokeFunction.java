package top.iot.protocol.godson.tcp.message;

import top.iot.protocol.godson.tcp.TcpPayload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.iot.gateway.core.message.function.FunctionInvokeMessage;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class InvokeFunction implements TcpPayload {
    private FunctionInvokeMessage functionInvokeMessage;

    @Override
    public byte[] toBytes() {
        return functionInvokeMessage.toString().getBytes();
    }
}
