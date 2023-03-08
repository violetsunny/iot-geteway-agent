package top.iot.gateway.network.udp.client;

import top.iot.gateway.network.Network;
import top.iot.gateway.network.udp.message.UdpMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UdpClient extends Network {
    Flux<UdpMessage> subscribe();

    Mono<Boolean> send(UdpMessage udpMessage);

    void onDisconnect(Runnable disconnected);

    void reset();
}
