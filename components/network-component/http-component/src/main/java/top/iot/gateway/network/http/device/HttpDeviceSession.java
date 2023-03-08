package top.iot.gateway.network.http.device;

import top.iot.gateway.component.gateway.monitor.DeviceGatewayMonitor;
import top.iot.gateway.network.http.client.HttpClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.iot.gateway.core.device.DeviceOperator;
import top.iot.gateway.core.message.codec.EncodedMessage;
import top.iot.gateway.core.message.codec.Transport;
import top.iot.gateway.core.message.codec.http.SimpleHttpRequestMessage;
import top.iot.gateway.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@Slf4j
class HttpDeviceSession implements DeviceSession {


    @Getter
    @Setter
    private DeviceOperator operator;

    @Setter
    private HttpClient client;

    @Getter
    private final Transport transport;

    private long lastPingTime = System.currentTimeMillis();

    private final long connectTime = System.currentTimeMillis();

    private final DeviceGatewayMonitor monitor;

    HttpDeviceSession(DeviceOperator operator,
                      HttpClient client,
                      Transport transport,
                      DeviceGatewayMonitor monitor) {
        this.operator = operator;
        this.client = client;
        this.transport = transport;
        this.monitor=monitor;
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
        return lastPingTime;
    }

    @Override
    public long connectTime() {
        return connectTime;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        monitor.sentMessage();
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
    public void setKeepAliveTimeout(Duration timeout) {
        client.setKeepAliveTimeout(timeout);
    }

    @Override
    public boolean isAlive() {
        log.info("device {} isAlive invoke", this.getDeviceId());
        return client.isAlive();
    }

    @Override
    public Optional<InetSocketAddress> getClientAddress() {
        return Optional.ofNullable(client.getRemoteAddress());
    }

    @Override
    public void onClose(Runnable call) {
        client.onDisconnect(call);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HttpDeviceSession session = (HttpDeviceSession) o;
        return Objects.equals(client, session.client);
    }

    @Override
    public int hashCode() {
        return Objects.hash(client);
    }
}
