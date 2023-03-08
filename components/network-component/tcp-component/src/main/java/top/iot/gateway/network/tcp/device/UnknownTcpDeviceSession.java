package top.iot.gateway.network.tcp.device;

import top.iot.gateway.network.tcp.TcpMessage;
import top.iot.gateway.network.tcp.client.TcpClient;
import lombok.Getter;
import top.iot.gateway.core.device.DeviceOperator;
import top.iot.gateway.core.message.codec.EncodedMessage;
import top.iot.gateway.core.message.codec.Transport;
import top.iot.gateway.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

class UnknownTcpDeviceSession implements DeviceSession {

    @Getter
    private final String id;

    private final TcpClient client;

    @Getter
    private final Transport transport;

    private long lastPingTime = System.currentTimeMillis();

    private final long connectTime = System.currentTimeMillis();

    UnknownTcpDeviceSession(String id, TcpClient client, Transport transport) {
        this.id = id;
        this.client = client;
        this.transport = transport;
    }

    @Override
    public String getDeviceId() {
        return "unknown";
    }

    @Override
    public DeviceOperator getOperator() {
        return null;
    }

    @Override
    public long lastPingTime() {
        return lastPingTime;
    }

    @Override
    public long connectTime() {
        return connectTime;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return client.send(new TcpMessage(encodedMessage.getPayload()));
    }

    @Override
    public void close() {
        client.shutdown();
    }

    @Override
    public void ping() {
        lastPingTime = System.currentTimeMillis();
        client.keepAlive();
    }

    @Override
    public boolean isAlive() {
        return client.isAlive();
    }

    @Override
    public void onClose(Runnable call) {
        client.onDisconnect(call);
    }
}
