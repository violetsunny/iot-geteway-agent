package top.iot.protocol.common.tcp.message;

import top.iot.protocol.common.tcp.TcpPayload;

public class Ping implements TcpPayload {
    @Override
    public byte[] toBytes() {
        return new byte[0];
    }

    @Override
    public void fromBytes(byte[] bytes, int offset) {

    }
}
