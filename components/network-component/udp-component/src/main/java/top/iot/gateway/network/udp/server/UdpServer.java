package top.iot.gateway.network.udp.server;

import top.iot.gateway.network.Network;
import top.iot.gateway.network.udp.client.UdpClient;
import reactor.core.publisher.Flux;

public interface UdpServer extends Network {

    void shutdown();

    Flux<UdpClient> handleConnection();


}
