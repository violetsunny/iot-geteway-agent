package top.iot.gateway.manager.debug;

import top.iot.gateway.component.gateway.external.SubscribeRequest;
import top.iot.gateway.component.gateway.external.SubscriptionProvider;
import top.iot.gateway.network.DefaultNetworkType;
import top.iot.gateway.network.NetworkManager;
import top.iot.gateway.network.udp.client.UdpClient;
import top.iot.gateway.network.udp.message.UdpMessage;
import io.netty.buffer.Unpooled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class UdpDebugSubscriptionProvider implements SubscriptionProvider {

    private final NetworkManager networkManager;

    public UdpDebugSubscriptionProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String id() {
        return "network-udp-debug";
    }

    @Override
    public String name() {
        return "UDP调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
                "/network/udp/*/_subscribe",
                "/network/udp/*/_send"
        };
    }

    @Override
    public Flux<String> subscribe(SubscribeRequest request) {
        String id = request.getTopic().split("[/]")[3];
        if (request.getTopic().endsWith("_send")) {
            return send(id, request);
        } else {
            return subscribe(id, request);
        }
    }

    public Flux<String> send(String id, SubscribeRequest request) {
        String message = request.getString("request")
                .orElseThrow(() -> new IllegalArgumentException("参数[request]不能为空"));
        String[] split = message.split("[\n]");
        if (split.length<=1 || !split[0].contains(":")) {
            throw new IllegalArgumentException("参数[request]格式不正确");
        }
        byte[] payload = DebugUtils.stringToBytes(message);

        return networkManager
                .<UdpClient>getNetwork(DefaultNetworkType.UDP, id)
                .flatMap(server -> server.send(new UdpMessage(split[0].split(":")[0], Integer.parseInt(split[0].split(":")[1]), Unpooled.wrappedBuffer(payload))))
                .thenReturn("推送成功")
                .flux();
    }

    @SuppressWarnings("all")
    public Flux<String> subscribe(String id, SubscribeRequest request) {
        return networkManager
                .<UdpClient>getNetwork(DefaultNetworkType.UDP, id)
                .flatMapMany(client -> client
                        .subscribe()
                        .map(UdpMessage::toString));

    }
}