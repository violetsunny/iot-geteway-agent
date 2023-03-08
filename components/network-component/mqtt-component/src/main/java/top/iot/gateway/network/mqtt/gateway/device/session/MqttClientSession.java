package top.iot.gateway.network.mqtt.gateway.device.session;

import top.iot.gateway.component.gateway.monitor.DeviceGatewayMonitor;
import top.iot.gateway.network.mqtt.client.MqttClient;
import lombok.Getter;
import lombok.Setter;
import top.iot.gateway.core.device.DeviceOperator;
import top.iot.gateway.core.message.codec.DefaultTransport;
import top.iot.gateway.core.message.codec.EncodedMessage;
import top.iot.gateway.core.message.codec.MqttMessage;
import top.iot.gateway.core.message.codec.Transport;
import top.iot.gateway.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class MqttClientSession implements DeviceSession {
    @Getter
    private final String id;

    @Getter
    private final DeviceOperator operator;

    @Getter
    @Setter
    private MqttClient client;

    private final long connectTime = System.currentTimeMillis();

    private long lastPingTime = System.currentTimeMillis();

    private long keepAliveTimeout = -1;

    private final DeviceGatewayMonitor monitor;

    public MqttClientSession(String id,
                             DeviceOperator operator,
                             MqttClient client,
                             DeviceGatewayMonitor monitor) {
        this.id = id;
        this.operator = operator;
        this.client = client;
        this.monitor=monitor;
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
        if (encodedMessage instanceof MqttMessage) {
            monitor.sentMessage();
            return client
                .publish(((MqttMessage) encodedMessage))
                .thenReturn(true)
                ;
        }
        return Mono.error(new UnsupportedOperationException("unsupported message type:" + encodedMessage.getClass()));
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.MQTT;
    }

    @Override
    public void close() {

    }

    @Override
    public void ping() {
        lastPingTime = System.currentTimeMillis();
    }

    @Override
    public boolean isAlive() {
        return client.isAlive() &&
            (keepAliveTimeout <= 0 || System.currentTimeMillis() - lastPingTime < keepAliveTimeout);
    }

    @Override
    public void onClose(Runnable call) {

    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        this.keepAliveTimeout = timeout.toMillis();
    }

    @Override
    public String toString() {
        return "MqttClientSession{" +
            "id=" + id + ",device=" + getDeviceId() +
            '}';
    }
}
