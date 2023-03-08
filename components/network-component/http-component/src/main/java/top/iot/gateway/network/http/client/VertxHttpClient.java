package top.iot.gateway.network.http.client;

import com.alibaba.fastjson.JSON;
import top.iot.gateway.network.DefaultNetworkType;
import top.iot.gateway.network.NetworkType;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpConnection;
import io.vertx.core.http.impl.HttpServerConnection;
import io.vertx.core.net.SocketAddress;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.iot.gateway.core.message.codec.http.HttpRequestMessage;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

@Slf4j
public class VertxHttpClient implements HttpClient {

    public volatile io.vertx.core.http.HttpClient client;

    public HttpServerConnection connection;

    @Getter
    private final String id;

    @Setter
    private long keepAliveTimeoutMs = Duration.ofMinutes(10).toMillis();

    private volatile long lastKeepAliveTime = System.currentTimeMillis();

    private final List<Runnable> disconnectListener = new CopyOnWriteArrayList<>();

    private final EmitterProcessor<HttpRequestMessage> processor = EmitterProcessor.create(false);

    private final FluxSink<HttpRequestMessage> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final boolean serverClient;

    @Override
    public void keepAlive() {
        lastKeepAliveTime = System.currentTimeMillis();
    }

    @Override
    public void setKeepAliveTimeout(Duration timeout) {
        keepAliveTimeoutMs = timeout.toMillis();
    }

    @Override
    public void reset() {

    }

    @Override
    public boolean isAlive() {
        log.info("connection is null:{}, keepAliveTimeoutMs:{}, lastKeepAliveTime:{}, System.currentTimeMillis() - lastKeepAliveTime:{}", connection==null, keepAliveTimeoutMs,lastKeepAliveTime, System.currentTimeMillis() - lastKeepAliveTime);
        return /*connection != null &&*/ (keepAliveTimeoutMs < 0 || System.currentTimeMillis() - lastKeepAliveTime < keepAliveTimeoutMs);
    }

    @Override
    public boolean isAutoReload() {
        return true;
    }

    public VertxHttpClient(String id, boolean serverClient) {
        this.id = id;
        this.serverClient=serverClient;
    }

    public void received(HttpRequestMessage message) {
        if (processor.getPending() > processor.getBufferSize() / 2) {
            log.warn("http [{}] message pending {} ,drop message:{}", processor.getPending(), getRemoteAddress(), message.toString());
            return;
        }
        log.info("connection remoteAddress:{}, port:{}", connection.remoteAddress().hostAddress(), connection.remoteAddress().port());
        log.info("VertxHttpClient emit message");
        sink.next(message);
    }

    @Override
    public Flux<HttpRequestMessage> subscribe() {
        return processor
            .map(Function.identity());
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("close http client error", e);
        }
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        if (null == connection) {
            return null;
        }
        SocketAddress connectionAddress = connection.remoteAddress();
        return new InetSocketAddress(connectionAddress.host(), connectionAddress.port());
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.HTTP_CLIENT;
    }

    @Override
    public void shutdown() {
        log.info("http client [{}] disconnect", getId());
        synchronized (this) {
            if (null != client) {
                execute(client::close);
                client = null;
            }
            if (null != connection) {
                execute(connection::close);
                this.connection = null;
            }
        }
        for (Runnable runnable : disconnectListener) {
            execute(runnable);
        }
        disconnectListener.clear();
        if(serverClient){
            processor.onComplete();
        }
    }

    public void setClient(io.vertx.core.http.HttpClient client) {
//        if (this.client != null && this.client != client) {
//            this.client.close();
//        }
        keepAlive();
        this.client = client;
    }

    public void setConnection(HttpConnection connection) {
        synchronized (this) {
            if (this.connection != null && this.connection != connection) {
                this.connection.close();
            }
            this.connection = (HttpServerConnection) connection
                .closeHandler(v -> shutdown());
//                .pingHandler(buffer -> {
//                    if (log.isDebugEnabled()) {
//                        log.debug("handle http client[{}] payload:[{}]",
//                            connection.remoteAddress(),
//                            Hex.encodeHexString(buffer.getBytes()));
//                    }
//                    keepAlive();
//                    if (this.connection != connection) {
//                        log.warn("http client [{}] memory leak ", connection.remoteAddress());
//                        connection.close();
//                    }
//                });
        }
    }

    @Override
    public Mono<Boolean> send(HttpRequestMessage message) {
        return Mono.<Boolean>create((sink) -> {
            if (connection == null) {
                sink.error(new SocketException("connection closed"));
                return;
            }
            log.info("http网关开始发送消息到设备");
//            Buffer buffer = Buffer.buffer(message.getPayload());
            Buffer buffer = Buffer.buffer(JSON.toJSONString(message.getQueryParameters()));
//            client.request(HttpMethod.valueOf(message.getMethod().name()), 80, "www.bzccspt.com", message.getPath()).write(buffer, r -> {
//                keepAlive();
//                if (r.succeeded()) {
//                    log.info("http网关发送消息成功");
//                    sink.success(true);
//                } else {
//                    log.info("http网关发送消息失败");
//                    sink.error(r.cause());
//                }
//            });
        });
    }

    @Override
    public void onDisconnect(Runnable disconnected) {
        disconnectListener.add(disconnected);
    }
}
