package top.iot.gateway.network.coap.server;

import top.iot.gateway.network.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import top.iot.gateway.core.metadata.ConfigMetadata;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Component
@Slf4j
@AllArgsConstructor
public class CoapServerProvider implements NetworkProvider<CoapServerProperties> {

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.COAP_SERVER;
    }

    @Nonnull
    @Override
    public Network createNetwork(@Nonnull CoapServerProperties properties) {
        return new DefaultCoapServer(properties);
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull CoapServerProperties properties) {
        DefaultCoapServer coapServer = (DefaultCoapServer) network;
        coapServer.shutdown();
        coapServer.createCoapServer(properties);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return null;
    }

    @Nonnull
    @Override
    public Mono<CoapServerProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            CoapServerProperties config = FastBeanCopier.copy(properties.getConfigurations(), new CoapServerProperties());
            config.setId(properties.getId());
            return Mono.just(config);
        });
    }
}
