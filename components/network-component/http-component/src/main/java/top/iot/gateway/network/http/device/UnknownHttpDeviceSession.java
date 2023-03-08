package top.iot.gateway.network.http.device;

import top.iot.gateway.network.http.client.HttpClient;
import lombok.Getter;
import lombok.Setter;
import top.iot.gateway.core.device.DeviceOperator;
import top.iot.gateway.core.message.codec.EncodedMessage;
import top.iot.gateway.core.message.codec.Transport;
import top.iot.gateway.core.message.codec.http.SimpleHttpRequestMessage;
import top.iot.gateway.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

class UnknownHttpDeviceSession implements DeviceSession {

    @Getter
    private final String id;

    private final HttpClient client;

    @Getter
    private final Transport transport;

    private long lastPingTime = System.currentTimeMillis();

    private final long connectTime = System.currentTimeMillis();

    @Getter
    @Setter
    private String deviceId;

    UnknownHttpDeviceSession(String id, HttpClient client, Transport transport) {
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
        if (encodedMessage instanceof SimpleHttpRequestMessage) {
            return client.send((SimpleHttpRequestMessage)encodedMessage);
        }
        return Mono.empty();
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
