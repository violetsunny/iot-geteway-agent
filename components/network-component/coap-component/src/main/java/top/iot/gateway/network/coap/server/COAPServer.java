package top.iot.gateway.network.coap.server;

import top.iot.gateway.network.Network;
import reactor.core.publisher.Flux;

public interface COAPServer extends Network {
    Flux<CoapConnection> handleConnection();
}
