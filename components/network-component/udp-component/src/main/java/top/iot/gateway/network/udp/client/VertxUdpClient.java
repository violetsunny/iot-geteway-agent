package top.iot.gateway.network.udp.client;

import top.iot.gateway.network.DefaultNetworkType;
import top.iot.gateway.network.NetworkType;
import top.iot.gateway.network.udp.message.UdpMessage;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramSocket;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

@Slf4j
public class VertxUdpClient implements UdpClient {

    @Getter
    private final String id;
    private final List<Runnable> disconnectListener = new CopyOnWriteArrayList<>();
    private DatagramSocket socket;

    private final EmitterProcessor<UdpMessage> processor = EmitterProcessor.create(false);
    private final FluxSink<UdpMessage> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    public VertxUdpClient(String id) {
        this.id = id;
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.UDP;
    }

    public void setSocket(DatagramSocket _socket, UdpClientProperties properties) {
        if (socket != null) {
            shutdown();
        }
        socket = _socket;
        socket.listen(properties.getLocalPort(), properties.getLocalAddress(), result -> {
            if (result.succeeded()) {
                log.info("udp socket startup on {}:{}", result.result().localAddress().host(), result.result().localAddress().port());
                socket.handler(packet -> {
                    if (this.socket != null && this.socket != _socket) {
                        this.socket.close();
                    }
                    sink.next(new UdpMessage(packet.data().getByteBuf()));
                });
            } else {
                log.error("startup udp socket error", result.cause());
            }
        });
    }

    @Override
    public boolean isAlive() {
        return socket != null;
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }

    @Override
    public Flux<UdpMessage> subscribe() {
        return processor
                .map(Function.identity());
    }

    @Override
    public Mono<Boolean> send(UdpMessage message) {
        return Mono
                .create((sink) -> {
                    if (socket == null) {
                        sink.error(new SocketException("socket closed"));
                        return;
                    }
                    Buffer buffer = Buffer.buffer(message.getPayload());
                    socket.send(buffer, message.getPort(), message.getHostname(), r -> {
                        if (r.succeeded()) {
                            sink.success();
                        } else {
                            sink.error(r.cause());
                        }
                    });
                }).thenReturn(true);
    }

    @Override
    public void reset() {

    }

    @Override
    public void onDisconnect(Runnable disconnected) {
        disconnectListener.add(disconnected);
    }

    @Override
    public void shutdown() {
        if (null != socket) {
            execute(socket::close);
            socket = null;
        }
        for (Runnable runnable : disconnectListener) {
            execute(runnable);
        }
        disconnectListener.clear();
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("close udp socket error", e);
        }
    }
}
