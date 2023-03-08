package top.iot.gateway.network.udp.device;

import top.iot.gateway.network.udp.client.UdpClient;
import lombok.Getter;
import lombok.Setter;
import top.iot.gateway.core.device.DeviceOperator;
import top.iot.gateway.core.message.codec.EncodedMessage;
import top.iot.gateway.core.message.codec.Transport;
import top.iot.gateway.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

class UnknownUdpDeviceSession implements DeviceSession {

    @Getter
    private final String id;

    private final UdpClient client;

    @Getter
    private final Transport transport;

    @Getter
    @Setter
    private String deviceId;

    UnknownUdpDeviceSession(String id, UdpClient client, Transport transport) {
        this.id = id;
        this.client = client;
        this.transport = transport;
    }

//    @Override
//    public String getDeviceId() {
//        return "unknown";
//    }

    @Override
    public DeviceOperator getOperator() {
        return null;
    }

    @Override
    public long lastPingTime() {
        return 0;
    }

    @Override
    public long connectTime() {
        return 0;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return Mono.empty();
    }

    @Override
    public void close() {
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