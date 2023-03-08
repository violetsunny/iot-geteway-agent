package top.iot.gateway.network.udp.client;

import top.iot.gateway.network.*;
import top.iot.gateway.network.security.CertificateManager;
import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocketOptions;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import top.iot.gateway.core.metadata.ConfigMetadata;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Component
@Slf4j
public class VertxUdpClientProvider implements NetworkProvider<UdpClientProperties> {

    private final CertificateManager certificateManager;

    private final Vertx vertx;

    public VertxUdpClientProvider(CertificateManager certificateManager, Vertx vertx) {
        this.certificateManager = certificateManager;
        this.vertx = vertx;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.UDP;
    }

    @Nonnull
    @Override
    public VertxUdpClient createNetwork(@Nonnull UdpClientProperties properties) {
        VertxUdpClient udpClient = new VertxUdpClient(properties.getId());
        udpClient.setSocket(vertx.createDatagramSocket(properties.getOptions()), properties);
        return udpClient;
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull UdpClientProperties properties) {
        VertxUdpClient udpClient = ((VertxUdpClient) network);
        udpClient.shutdown();
        udpClient.setSocket(vertx.createDatagramSocket(properties.getOptions()), properties);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return null;
    }

    @Nonnull
    @Override
    public Mono<UdpClientProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            UdpClientProperties config = FastBeanCopier.copy(properties.getConfigurations(), new UdpClientProperties());
            config.setId(properties.getId());
            if (config.getOptions() == null) {
                config.setOptions(new DatagramSocketOptions());
            }
            return Mono.just(config);
        });
    }
}
