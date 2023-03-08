package top.iot.gateway.network.http.client;

import top.iot.gateway.network.*;
import top.iot.gateway.network.security.CertificateManager;
import top.iot.gateway.network.security.VertxKeyCertTrustOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import top.iot.gateway.core.metadata.ConfigMetadata;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;

//@Component
@Slf4j
public class VertxHttpClientProvider implements NetworkProvider<HttpClientProperties> {

    private final CertificateManager certificateManager;

    private final Vertx vertx;

    public VertxHttpClientProvider(CertificateManager certificateManager, Vertx vertx) {
        this.certificateManager = certificateManager;
        this.vertx = vertx;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.HTTP_CLIENT;
    }

    @Nonnull
    @Override
    public VertxHttpClient createNetwork(@Nonnull HttpClientProperties properties) {
        VertxHttpClient client = new VertxHttpClient(properties.getId(),false);

        initClient(client, properties);

        return client;
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull HttpClientProperties properties) {
        initClient(((VertxHttpClient) network), properties);
    }

    public void initClient(VertxHttpClient client, HttpClientProperties properties) {
        HttpClient httpClient = vertx.createHttpClient(properties.getOptions());
        client.setClient(httpClient);
        client.setKeepAliveTimeoutMs(properties.getLong("keepAliveTimeout").orElse(Duration.ofMinutes(10).toMillis()));
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        // TODO: 2019/12/19
        return null;
    }

    @Nonnull
    @Override
    public Mono<HttpClientProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            HttpClientProperties config = FastBeanCopier.copy(properties, new HttpClientProperties());
            config.setId(properties.getId());
            if (config.getOptions() == null) {
                config.setOptions(new HttpClientOptions());
            }
            if (config.isShareCluster()) {
                if ((boolean)config.getConfigurations().get("ssl")) {
                    config.getOptions().setSsl(true);
                    return certificateManager.getCertificate(String.valueOf(config.getConfigurations().get("certId")))
                        .map(VertxKeyCertTrustOptions::new)
                        .doOnNext(config.getOptions()::setKeyCertOptions)
                        .doOnNext(config.getOptions()::setTrustOptions)
                        .thenReturn(config);
                }
            }
            return Mono.just(config);
        });
    }
}
