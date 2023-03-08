package top.iot.gateway.network.http.server;

import top.iot.gateway.network.http.client.HttpClient;
import top.iot.gateway.network.http.client.VertxHttpClient;
import top.iot.gateway.network.DefaultNetworkType;
import top.iot.gateway.network.NetworkType;
import io.vertx.core.http.HttpConnection;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;

@Slf4j
public class VertxHttpServer implements HttpServer {

    @Getter
    @Setter
    io.vertx.core.http.HttpServer httpServer;

    HttpClient httpClient;

    @Setter
    private long keepAliveTimeout = Duration.ofMinutes(10).toMillis();

    @Getter
    private final String id;

    private final EmitterProcessor<HttpClient> processor = EmitterProcessor.create(false);

    private final FluxSink<HttpClient> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);


    VertxHttpServer(String id) {
        this.id = id;
    }


    @Override
    public Flux<HttpClient> handleConnection() {
        return processor
            .map(i -> i);
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("close http server error", e);
        }
    }

    public void setServer(io.vertx.core.http.HttpServer httpServer) {
        if (this.httpServer != null) {
            shutdown();
        }
        this.httpServer = httpServer;
        this.httpServer.connectionHandler(this::acceptHttpConnection)
                .requestHandler(request -> SimpleHttpExchangeMessage.of(request, httpClient::received))
                .exceptionHandler(error -> log.error(error.getMessage(), error));
    }

    private void acceptHttpConnection(HttpConnection connection) {
        if (!processor.hasDownstreams()) {
            log.warn("not handler for http client[{}]", connection.remoteAddress());
            connection.close();
            return;
        }
        VertxHttpClient client = new VertxHttpClient(id + "_" + connection.remoteAddress(), true);
        client.setKeepAliveTimeoutMs(keepAliveTimeout);
        try {
            connection.exceptionHandler(err -> {
                log.error("http server client [{}] error", connection.remoteAddress(), err);
            }).closeHandler((nil) -> {
                log.info("http server client [{}] closed", connection.remoteAddress());
                client.shutdown();
            });
            client.setConnection(connection);
            this.httpClient = client;
            sink.next(client);
            log.info("accept http client [{}] connection", connection.remoteAddress());
        } catch (Exception e) {
            log.error("create http server client error", e);
            client.shutdown();
        }
    }


    @Override
    public NetworkType getType() {
        return DefaultNetworkType.HTTP_SERVER;
    }

    @Override
    public void shutdown() {
        if (null != httpServer) {
            execute(httpServer::close);
            httpServer = null;
        }
    }

    @Override
    public boolean isAlive() {
        return httpServer != null;
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }
}
