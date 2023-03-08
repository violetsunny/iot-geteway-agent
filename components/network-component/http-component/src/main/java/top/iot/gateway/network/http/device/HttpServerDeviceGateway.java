package top.iot.gateway.network.http.device;

import com.alibaba.fastjson.JSON;
import top.iot.gateway.component.elasticsearch.index.ElasticIndex;
import top.iot.gateway.component.elasticsearch.index.EsSourceData;
import top.iot.gateway.component.elasticsearch.service.ElasticSearchService;
import top.iot.gateway.component.gateway.DeviceGateway;
import top.iot.gateway.component.gateway.monitor.DeviceGatewayMonitor;
import top.iot.gateway.component.gateway.monitor.GatewayMonitors;
import top.iot.gateway.component.gateway.monitor.MonitorSupportDeviceGateway;
import top.iot.gateway.network.DefaultNetworkType;
import top.iot.gateway.network.DeviceMessageIndex;
import top.iot.gateway.network.NetworkType;
import top.iot.gateway.network.http.client.HttpClient;
import top.iot.gateway.network.http.server.HttpServer;
import top.iot.gateway.network.mq.devtype.IotDevTypeProducer;
import top.iot.gateway.network.utils.DeviceGatewayHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import top.iot.gateway.core.ProtocolSupports;
import top.iot.gateway.core.device.DeviceRegistry;
import top.iot.gateway.core.device.session.DeviceSessionManager;
import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.codec.DefaultTransport;
import top.iot.gateway.core.message.codec.FromDeviceMessageContext;
import top.iot.gateway.core.message.codec.Transport;
import top.iot.gateway.core.message.codec.http.HttpRequestMessage;
import top.iot.gateway.core.server.session.DeviceSession;
import top.iot.gateway.supports.server.DecodedClientMessageHandler;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
public class HttpServerDeviceGateway implements DeviceGateway, MonitorSupportDeviceGateway {

    @Getter
    private final String id;

    private final HttpServer httpServer;

    private final List<GateWayRouter> gateWayRouters;

    private final ProtocolSupports supports;

    private final DeviceRegistry registry;

    private final DeviceGatewayMonitor gatewayMonitor;

    private final LongAdder counter = new LongAdder();

    private final EmitterProcessor<Message> processor = EmitterProcessor.create(false);

    private final FluxSink<Message> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final AtomicBoolean started = new AtomicBoolean();

    private final DeviceGatewayHelper helper;

    private final AtomicBoolean paused = new AtomicBoolean();

    private Disposable disposable;

    private PathMatcher pathMatcher = new AntPathMatcher();

    private IotDevTypeProducer iotDevTypeProducer;

    private final ElasticSearchService elasticSearchService;

    HttpServerDeviceGateway(String id,
                            List<GateWayRouter> gateWayRouters,
                            ProtocolSupports supports,
                            DeviceRegistry deviceRegistry,
                            DecodedClientMessageHandler clientMessageHandler,
                            DeviceSessionManager sessionManager,
                            HttpServer httpServer,
                            ElasticSearchService elasticSearchService,
                            IotDevTypeProducer iotDevTypeProducer) {
        this.gatewayMonitor = GatewayMonitors.getDeviceGatewayMonitor(id);
        this.id = id;
        this.gateWayRouters = gateWayRouters;
        this.registry = deviceRegistry;
        this.supports = supports;
        this.httpServer = httpServer;
        this.helper = new DeviceGatewayHelper(registry, sessionManager, clientMessageHandler);
        this.iotDevTypeProducer = iotDevTypeProducer;
        this.elasticSearchService = elasticSearchService;
    }


    @Override
    public Transport getTransport() {
        return DefaultTransport.HTTP;
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.HTTP_SERVER;
    }

    @Override
    public Flux<Message> onMessage() {
        return processor;
    }

    @Override
    public Mono<Void> startup() {
        return Mono.fromRunnable(this::doStart);
    }

    private void doStart() {
        if (started.getAndSet(true) || disposable != null) {
            return;
        }
        disposable = httpServer
                .handleConnection()
                .publishOn(Schedulers.parallel())
                .flatMap(client -> new HttpConnection(client).accept(), Integer.MAX_VALUE)
                .onErrorContinue((err, obj) -> log.error(err.getMessage(), err))
                .subscriberContext(ReactiveLogger.start("network", httpServer.getId()))
                .subscribe(
                        ignore -> {
                        },
                        error -> log.error(error.getMessage(), error)
                );
    }

    @Override
    public Mono<Void> pause() {
        return Mono.fromRunnable(() -> {
            started.set(false);
        });

    }

    @Override
    public Mono<Void> shutdown() {
        return Mono.fromRunnable(() -> {
            started.set(false);
            disposable.dispose();
            disposable = null;
        });
    }

    @Override
    public boolean isAlive() {
        return started.get();
    }

    @Override
    public long totalConnection() {
        return counter.sum();
    }

    class HttpConnection {
        final HttpClient client;
        final AtomicReference<Duration> keepaliveTimeout = new AtomicReference<>();
        final AtomicReference<DeviceSession> sessionRef = new AtomicReference<>();
        final InetSocketAddress address;

        HttpConnection(HttpClient client) {
            this.client = client;
            this.address = client.getRemoteAddress();
            gatewayMonitor.totalConnection(counter.sum());

            client.onDisconnect(() -> {
                counter.decrement();
                gatewayMonitor.disconnected();
                gatewayMonitor.totalConnection(counter.sum());
            });
            gatewayMonitor.connected();
            sessionRef.set(new UnknownHttpDeviceSession(client.getId(), client, getTransport()));

        }

        Mono<Void> accept() {
            return client
                    .subscribe()
                    .filter(http -> started.get())
                    .publishOn(Schedulers.parallel())
                    .flatMap(this::handleHttpMessage)
                    .onErrorResume((err) -> {
                        log.error(err.getMessage(), err);
                        client.shutdown();
                        return Mono.empty();
                    })
                    .then()
                    .doOnCancel(client::shutdown);
        }

        private Mono<Void> handleHttpMessage(HttpRequestMessage message) {
            log.info("handleHttpMessage调用");
            GateWayRouter router = gateWayRouters.stream().filter(r -> pathMatcher.match(r.getUrl(), message.getPath())).findFirst().orElseThrow(() -> new RuntimeException("路径不匹配"));
            return supports.getProtocol(router.getProtocol())
                    .flatMap(pt -> pt.getMessageCodec(getTransport()))
                    .flatMapMany(codec -> codec.decode(FromDeviceMessageContext.of(sessionRef.get(), message, registry)))
                    .cast(DeviceMessage.class)
                    .doOnNext(msg -> gatewayMonitor.receivedMessage())
                    .flatMap(msg -> handleDeviceMessage(msg, router.getProtocol()))
                    .flatMap(msg -> {
                        log.info("HttpServer 发送消息给下游：{}", JSON.toJSONString(msg));
                        return iotDevTypeProducer.sendMessage(msg).thenReturn(msg);
                    })
                    .doOnEach(ReactiveLogger.onError(err -> log.error("处理HTTP[{}]消息失败:\n{}", address, message, err)))
                    .onErrorResume((err) -> Mono.fromRunnable(client::reset))
                    .flatMap(msg -> {
                        String url = message.getUrl();
                        //todo 根据url区分logType
                        String logType = "metric";
                        if (url.endsWith("/device/realtime")) {
                            logType = "metric";
                        }
                        ElasticIndex elasticIndex = new DeviceMessageIndex(logType, System.currentTimeMillis());
                        return elasticSearchService.commit(elasticIndex, Mono.just(new EsSourceData(msg.getDeviceId(), "rtg", message.getPayload().toString(StandardCharsets.UTF_8))))
                                .onErrorContinue((err, obj) -> log.error("es处理失败", err))
                                .thenReturn(msg);
                    })
                    .then();
        }

        private Mono<DeviceMessage> handleDeviceMessage(DeviceMessage message, String protocol) {
            if (processor.hasDownstreams()) {
                sink.next(message);
            }
            return helper
                    .handleDeviceMessage(message,
                            device -> {
                                Mono.defer(() -> {
                                    Map<String, Object> configs = new HashMap<>();
                                    configs.put("protocol", protocol);
                                    configs.put("transport", getTransport().getId());
                                    return Mono.just(configs);
                                }).flatMap(device::setConfigs).subscribe();
                                return new HttpDeviceSession(device, client, getTransport(), gatewayMonitor);
                            },
                            DeviceGatewayHelper
                                    .applySessionKeepaliveTimeout(message, keepaliveTimeout::get)
                                    .andThen(session -> {
                                        HttpDeviceSession deviceSession = session.unwrap(HttpDeviceSession.class);
                                        deviceSession.setClient(client);
                                        sessionRef.set(deviceSession);
                                    }),
                            () -> log.warn("无法从http[{}]消息中获取设备信息:{}", address, message)
                    )
                    .thenReturn(message);
        }
    }
}
