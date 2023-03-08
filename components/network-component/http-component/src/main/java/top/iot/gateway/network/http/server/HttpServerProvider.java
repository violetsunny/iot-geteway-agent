package top.iot.gateway.network.http.server;

import top.iot.gateway.network.*;
import top.iot.gateway.network.security.CertificateManager;
import top.iot.gateway.network.security.VertxKeyCertTrustOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import top.iot.gateway.core.metadata.ConfigMetadata;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Component
@Slf4j
public class HttpServerProvider implements NetworkProvider<HttpServerProperties> {

    private final CertificateManager certificateManager;

    private final Vertx vertx;


    public HttpServerProvider(CertificateManager certificateManager, Vertx vertx) {
        this.certificateManager = certificateManager;
        this.vertx = vertx;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.HTTP_SERVER;
    }

    @Nonnull
    @Override
    public VertxHttpServer createNetwork(@Nonnull HttpServerProperties properties) {

        VertxHttpServer httpServer = new VertxHttpServer(properties.getId());
        initHttpServer(httpServer, properties);

        return httpServer;
    }

    private void initHttpServer(VertxHttpServer httpServer, HttpServerProperties properties) {
        HttpServer server = vertx.createHttpServer(properties.getOptions());
        server.requestHandler(request -> request.response().end());
        server.exceptionHandler(error -> {
            log.error(error.getMessage(), error);
        });
        httpServer.setServer(server);
        server.listen(properties.createSocketAddress(), result -> {
            if (result.succeeded()) {
                log.info("http server startup on {}", result.result().actualPort());
            } else {
                log.error("startup http server error", result.cause());
            }
        });
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull HttpServerProperties properties) {
        VertxHttpServer httpServer = ((VertxHttpServer) network);
        httpServer.shutdown();
        initHttpServer(httpServer, properties);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return null;
    }

    @Nonnull
    @Override
    public Mono<HttpServerProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            HttpServerProperties config = FastBeanCopier.copy(properties, new HttpServerProperties());
//            config.setId(properties.getId());
            if (config.getOptions() == null) {
                config.setOptions(new HttpServerOptions());
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
