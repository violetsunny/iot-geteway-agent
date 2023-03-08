package top.iot.gateway.network.coap.device;

import top.iot.gateway.component.gateway.monitor.DeviceGatewayMonitor;
import top.iot.gateway.network.coap.server.CoapConnection;
import lombok.Getter;
import lombok.Setter;
import top.iot.gateway.core.device.DeviceOperator;
import top.iot.gateway.core.message.codec.EncodedMessage;
import top.iot.gateway.core.message.codec.Transport;
import top.iot.gateway.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

class CoapDeviceSession implements DeviceSession {

    private DeviceOperator operator;

    @Setter
    private CoapConnection connection;

    private DeviceGatewayMonitor monitor;

    @Getter
    private final Transport transport;

    private long lastPingTime = System.currentTimeMillis();

    private final long connectTime = System.currentTimeMillis();

    CoapDeviceSession(DeviceOperator operator,
                      CoapConnection connection,
                      Transport transport,
                      DeviceGatewayMonitor monitor) {
        this.operator = operator;
        this.connection = connection;
        this.transport = transport;
        this.monitor=monitor;
    }

    @Override
    public String getId() {
        return getDeviceId();
    }

    @Override
    public String getDeviceId() {
        if (operator == null) {
            return null;
        }
        return operator.getDeviceId();
    }

    @Override
    public DeviceOperator getOperator() {
        return operator;
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
        return null;
//        return client.send(new TcpMessage(encodedMessage.getPayload()));
    }

    @Override
    public void close() {
        connection.close().subscribe();
    }

    @Override
    public void ping() {
        lastPingTime = System.currentTimeMillis();
        connection.keepAlive();
    }

    @Override
    public boolean isAlive() {
        return connection.isAlive();
    }

    @Override
    public void onClose(Runnable call) {
        connection.onClose(c -> call.run());
    }
}
