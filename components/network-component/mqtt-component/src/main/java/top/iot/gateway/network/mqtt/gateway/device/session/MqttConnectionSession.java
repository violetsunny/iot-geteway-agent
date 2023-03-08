package top.iot.gateway.network.mqtt.gateway.device.session;

import top.iot.gateway.network.mqtt.server.MqttConnection;
import com.fasterxml.jackson.annotation.JsonIgnore;
import top.iot.gateway.component.gateway.monitor.DeviceGatewayMonitor;
import lombok.Getter;
import top.iot.gateway.core.device.DeviceOperator;
import top.iot.gateway.core.message.codec.EncodedMessage;
import top.iot.gateway.core.message.codec.MqttMessage;
import top.iot.gateway.core.message.codec.Transport;
import top.iot.gateway.core.server.session.DeviceSession;
import top.iot.gateway.core.server.session.ReplaceableDeviceSession;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class MqttConnectionSession implements DeviceSession, ReplaceableDeviceSession {

    @Getter
    private final String id;

    @Getter
    @JsonIgnore
    private final DeviceOperator operator;

    @Getter
    private final Transport transport;

    @Getter
    private MqttConnection connection;

    private final DeviceGatewayMonitor monitor;

    @Getter
    private final long connectTime = System.currentTimeMillis();

    private String serverId;

    public MqttConnectionSession(String id,
                                 DeviceOperator operator,
                                 Transport transport,
                                 MqttConnection connection,
                                 DeviceGatewayMonitor monitor) {
        this.id = id;
        this.operator = operator;
        this.transport = transport;
        this.connection = connection;
        this.monitor = monitor;
    }


    @Override
    public String getDeviceId() {
        return id;
    }

    @Override
    public long lastPingTime() {
        return connection.getLastPingTime();
    }

    @Override
    public long connectTime() {
        return connectTime;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return Mono.defer(() -> connection.publish(((MqttMessage) encodedMessage)))
                .doOnSuccess(nil -> monitor.sentMessage())
                .thenReturn(true);
    }

    @Override
    public void close() {
        connection.close().subscribe();
    }

    @Override
    public void ping() {
        connection.keepAlive();
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        connection.setKeepAliveTimeout(timeout);
    }

    @Override
    public boolean isAlive() {
        return connection.isAlive();
    }

    @Override
    public void onClose(Runnable call) {
        connection.onClose(c -> call.run());
    }

    @Override
    public Optional<InetSocketAddress> getClientAddress() {
        return Optional.ofNullable(connection.getClientAddress());
    }

    @Override
    public void replaceWith(DeviceSession session) {
        if (session instanceof MqttConnectionSession) {
            MqttConnectionSession connectionSession = ((MqttConnectionSession) session);
            this.connection = connectionSession.connection;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MqttConnectionSession that = (MqttConnectionSession) o;
        return Objects.equals(connection, that.connection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connection);
    }


    @Override
    public Optional<String> getServerId() {
        return Optional.ofNullable(serverId);
    }
}
