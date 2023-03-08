package top.iot.gateway.network.udp.device;

import top.iot.gateway.network.udp.message.UdpMessage;
import top.iot.gateway.network.udp.client.UdpClient;
import lombok.Getter;
import lombok.Setter;
import top.iot.gateway.core.device.DeviceOperator;
import top.iot.gateway.core.message.codec.EncodedMessage;
import top.iot.gateway.core.message.codec.Transport;
import top.iot.gateway.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

public class UdpDeviceSession implements DeviceSession {

    @Getter
    @Setter
    private DeviceOperator operator;

    @Setter
    private UdpClient client;

    @Getter
    private final Transport transport;

    public UdpDeviceSession(DeviceOperator operator, UdpClient client, Transport transport) {
        this.operator = operator;
        this.client = client;
        this.transport = transport;
    }

    @Override
    public String getId() {
        return getDeviceId();
    }

    @Override
    public String getDeviceId() {
        return operator.getDeviceId();
    }

    @Override
    public long lastPingTime() {
        return 0L;
    }

    @Override
    public long connectTime() {
        return 0L;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return client.send(new UdpMessage(encodedMessage.getPayload()));
    }

    @Override
    public void close() {
        client.shutdown();
    }

    @Override
    public void ping() {
    }

    @Override
    public boolean isAlive() {
        return false;
    }

    @Override
    public void onClose(Runnable call) {

    }
}
