package top.iot.gateway.network.tcp.device;

import com.alibaba.fastjson.JSON;
import top.iot.gateway.component.elasticsearch.index.ElasticIndex;
import top.iot.gateway.component.elasticsearch.index.EsSourceData;
import top.iot.gateway.component.elasticsearch.service.ElasticSearchService;
import top.iot.gateway.component.gateway.AbstractDeviceGateway;
import top.iot.gateway.component.gateway.monitor.MonitorSupportDeviceGateway;
import top.iot.gateway.network.*;
import top.iot.gateway.network.mq.devtype.IotDevTypeProducer;
import top.iot.gateway.network.tcp.TcpMessage;
import top.iot.gateway.network.tcp.client.TcpClient;
import top.iot.gateway.network.tcp.server.TcpServer;
import top.iot.gateway.network.utils.DeviceGatewayHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.logger.ReactiveLogger;
import top.iot.gateway.core.ProtocolSupport;
import top.iot.gateway.core.ProtocolSupports;
import top.iot.gateway.core.Value;
import top.iot.gateway.core.device.DeviceOperator;
import top.iot.gateway.core.device.DeviceProductOperator;
import top.iot.gateway.core.device.DeviceRegistry;
import top.iot.gateway.core.device.session.DeviceSessionManager;
import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.message.codec.DefaultTransport;
import top.iot.gateway.core.message.codec.EncodedMessage;
import top.iot.gateway.core.message.codec.FromDeviceMessageContext;
import top.iot.gateway.core.message.codec.Transport;
import top.iot.gateway.core.server.DeviceGatewayContext;
import top.iot.gateway.core.server.session.DeviceSession;
import top.iot.gateway.core.trace.DeviceTracer;
import top.iot.gateway.core.trace.MonoTracer;
import top.iot.gateway.supports.server.DecodedClientMessageHandler;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

@Slf4j(topic = "system.tcp.gateway")
class TcpServerDeviceGateway extends AbstractDeviceGateway implements MonitorSupportDeviceGateway {

    private final TcpServer tcpServer;

    private final String protocol;

    private final ProtocolSupports supports;

    private final DeviceRegistry registry;

    private final top.iot.gateway.core.device.session.DeviceSessionManager sessionManager;

    private final LongAdder counter = new LongAdder();

    private Disposable disposable;

    private final DeviceGatewayHelper helper;

    private final String deviceCode;

    private final SessionIdDeviceIdBindingRegistry sessionIdDeviceIdBindingRegistry;

    private final ElasticSearchService elasticSearchService;

    private IotDevTypeProducer iotDevTypeProducer;

    public TcpServerDeviceGateway(String id,
                                  String protocol,
                                  ProtocolSupports supports,
                                  DeviceRegistry deviceRegistry,
                                  DecodedClientMessageHandler clientMessageHandler,
                                  DeviceSessionManager sessionManager,
                                  TcpServer tcpServer,
                                  ElasticSearchService elasticSearchService,
                                  String deviceCode,
                                  SessionIdDeviceIdBindingRegistry sessionIdDeviceIdBindingRegistry,
                                  IotDevTypeProducer iotDevTypeProducer) {
        super(id);
        this.protocol = protocol;
        this.registry = deviceRegistry;
        this.supports = supports;
        this.tcpServer = tcpServer;
        this.sessionManager = sessionManager;
        this.helper = new DeviceGatewayHelper(registry, sessionManager, clientMessageHandler);
        this.elasticSearchService = elasticSearchService;
        this.deviceCode = deviceCode;
        this.sessionIdDeviceIdBindingRegistry = sessionIdDeviceIdBindingRegistry;
        this.iotDevTypeProducer = iotDevTypeProducer;
    }

    public Mono<ProtocolSupport> getProtocol() {
        return supports.getProtocol(protocol);
    }

    @Override
    public long totalConnection() {
        return counter.sum();
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.TCP;
    }

    /**
     * 网络类型
     *
     * @return {@link  DefaultNetworkType}
     */
    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.TCP_SERVER;
    }

    /**
     * TCP 客户端连接
     */
    class TcpConnection implements DeviceGatewayContext {
        final TcpClient client;
        final AtomicReference<Duration> keepaliveTimeout = new AtomicReference<>();
        final AtomicReference<DeviceSession> sessionRef = new AtomicReference<>();
        final InetSocketAddress address;

        TcpConnection(TcpClient client) {
            this.client = client;
            this.address = client.getRemoteAddress();
            monitor.totalConnection(counter.sum());
            client.onDisconnect(() -> {
                sessionIdDeviceIdBindingRegistry.removeConfig(client.getId())
                        .doOnNext(r -> {
                            counter.decrement();
                            monitor.disconnected();
                            monitor.totalConnection(counter.sum());
                            //check session
                            sessionManager
                                    .getSession(client.getId())
                                    .subscribe();
                        }).subscribe();
            });
            monitor.connected();
            sessionRef.set(new UnknownTcpDeviceSession(client.getId(), client, getTransport()));
        }

        Mono<Void> accept() {
            return getProtocol()
                    .flatMap(protocol -> protocol.onClientConnect(getTransport(), client, this))
                    .then(
                            client
                                    .subscribe()
                                    .filter(tcp -> isStarted())
                                    .publishOn(Schedulers.parallel())
                                    .flatMap(this::handleTcpMessage)
                                    .onErrorResume((err) -> {
                                        log.error(err.getMessage(), err);
                                        client.shutdown();
                                        return Mono.empty();
                                    })
                                    .then()
                    )
                    .doOnCancel(client::shutdown);
        }

        Mono<Void> handleTcpMessage(TcpMessage message) {
            long time = System.nanoTime();
            EncodedMessage tcpMessage = EncodedMessage.simple(message.getPayload());
            //deviceCode 放着怎么获取deviceId
            if (StringUtils.hasText(deviceCode)) {
                return sessionIdDeviceIdBindingRegistry.getConfig(client.getId(), false)
                        .map(Value::asString)
                        .switchIfEmpty(Mono.defer(() -> {
                            String deviceId = getDeviceId(tcpMessage, deviceCode);
                            //缓存中存放client.getId()和deviceId映射关系
                            return sessionIdDeviceIdBindingRegistry.setConfig(client.getId(), deviceId).thenReturn(deviceId);
                        }))
                        .flatMap(deviceId -> {
                                    // 修改es索引  DeviceMessageIndexProvider.DEVICE_ORIGINAL_MESSAGE
                                    String logType = "metric";
                                    ElasticIndex elasticIndex = new DeviceMessageIndex(logType, System.currentTimeMillis());
                                    // Mono.just(new EsSourceData(deviceId, "rtg", Hex.encodeHexString(message.payloadAsBytes())))
                                    return elasticSearchService.commit(elasticIndex, Mono.just(new EsSourceData(deviceId, "rtg", Hex.encodeHexString(message.payloadAsBytes()))))
                                            .onErrorContinue((err, obj) -> log.error("es处理失败", err))
                                            .then(
                                                    getProtocol()
                                                            .flatMap(pt -> pt.getMessageCodec(getTransport()))
//                                    .flatMapMany(codec -> codec.decode(FromDeviceMessageContext.of(sessionRef.get(), message, registry)))
                                                            .flatMapMany(codec -> codec.decode(new FromDeviceMessageContext() {
                                                                @Override
                                                                public DeviceSession getSession() {
                                                                    return sessionRef.get();
                                                                }

                                                                @Nonnull
                                                                @Override
                                                                public EncodedMessage getMessage() {
                                                                    return EncodedMessage.simple(message.getPayload());
                                                                }

                                                                @Override
                                                                public Mono<DeviceOperator> getDeviceAsync() {
                                                                    return registry.getDevice(deviceId).switchIfEmpty(Mono.fromRunnable(() -> log.warn("device [{}] not fond in registry", deviceId)));
                                                                }
                                                            }))
                                                            .cast(DeviceMessage.class)
                                                            .flatMap(msg -> this
                                                                    .handleDeviceMessage(msg)
                                                                    .as(MonoTracer.create(
                                                                            DeviceTracer.SpanName.decode(msg.getDeviceId()),
                                                                            builder -> {
                                                                                builder.setAttribute(DeviceTracer.SpanKey.message, msg.toString());
                                                                                builder.setStartTimestamp(time, TimeUnit.NANOSECONDS);
                                                                            })))
                                                            .flatMap(msg -> {
                                                                log.info("TCPServer 发送消息给下游：{}", JSON.toJSONString(msg));
                                                                return iotDevTypeProducer.sendMessage(msg).thenReturn(msg);
                                                            })
                                                            .doOnEach(ReactiveLogger
                                                                    .onError(err -> log.error("Handle TCP[{}] message failed:\n{}",
                                                                            address,
                                                                            message
                                                                            , err)))
                                                            .onErrorResume((err) -> Mono.fromRunnable(client::reset))
                                                            .then()
                                            );
                                }
                        ).then();
            } else {
                return getProtocol()
                        .flatMap(pt -> {
                            if (pt == null) {
                                return Mono.error(new NotFoundException("不支持的protocol:[" + protocol + "]"));
                            }
                            return pt.getMessageCodec(getTransport());
                        })
                        .flatMapMany(codec -> codec.decode(FromDeviceMessageContext.of(sessionRef.get(), message, registry)))
                        .cast(DeviceMessage.class)
                        .flatMap(msg -> this
                                .handleDeviceMessage(msg)
                                .as(MonoTracer.create(
                                        DeviceTracer.SpanName.decode(msg.getDeviceId()),
                                        builder -> {
                                            builder.setAttribute(DeviceTracer.SpanKey.message, msg.toString());
                                            builder.setStartTimestamp(time, TimeUnit.NANOSECONDS);
                                        })))
                        .flatMap(msg -> {
                            log.info("TCPServer 发送消息给下游：{}", JSON.toJSONString(msg));
                            return iotDevTypeProducer.sendMessage(msg).thenReturn(msg);
                        })
                        .doOnEach(ReactiveLogger
                                .onError(err -> log.error("Handle TCP[{}] message failed:\n{}",
                                        address,
                                        message
                                        , err)))
                        .onErrorResume((err) -> Mono.fromRunnable(client::reset))
                        .flatMap(msg -> {
                            String logType = "metric";
                            ElasticIndex elasticIndex = new DeviceMessageIndex(logType, System.currentTimeMillis());
                            //Hex.encodeHexString(message.payloadAsBytes())
                            return elasticSearchService.commit(elasticIndex, Mono.just(new EsSourceData(msg.getDeviceId(), "rtg", tcpMessage.payloadAsString())))
                                    .onErrorContinue((err, obj) -> log.error("es处理失败", err))
                                    .thenReturn(msg);
                        })
                        .subscribeOn(Schedulers.parallel())
                        .then();
            }
        }

        /**
         * 处理设备消息
         *
         * @param message 设备消息
         * @return void
         */
        Mono<DeviceMessage> handleDeviceMessage(DeviceMessage message) {
            monitor.receivedMessage();
            return helper
                    .handleDeviceMessage(message,
                            device -> {
                                Mono.defer(() -> {
                                    Map<String, Object> configs = new HashMap<>();
                                    configs.put("protocol", protocol);
                                    configs.put("transport", getTransport().getId());
                                    return Mono.just(configs);
                                }).flatMap(device::setConfigs).subscribe();
                                return new TcpDeviceSession(device, client, getTransport(), monitor);
                            },
                            session -> {
                                TcpDeviceSession deviceSession = session.unwrap(TcpDeviceSession.class);
                                deviceSession.setClient(client);
                                sessionRef.set(deviceSession);
                            },
                            () -> log.warn("TCP{}: The device[{}] in the message body does not exist:{}", address, message.getDeviceId(), message)
                    )
                    .thenReturn(message);
        }

        @Override
        public Mono<DeviceOperator> getDevice(String deviceId) {
            return registry.getDevice(deviceId).switchIfEmpty(Mono.fromRunnable(() -> log.warn("device [{}] not fond in registry", deviceId)));
        }

        @Override
        public Mono<DeviceProductOperator> getProduct(String productId) {
            return registry.getProduct(productId);
        }

        @Override
        public Mono<Void> onMessage(DeviceMessage message) {
            return handleDeviceMessage(message).then();
        }
    }

    private void doStart() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = tcpServer
                .handleConnection()
                .publishOn(Schedulers.parallel())
                .flatMap(client -> new TcpConnection(client)
                                .accept()
                                .onErrorResume(err -> {
                                    log.error("handle tcp client[{}] error", client.getRemoteAddress(), err);
                                    return Mono.empty();
                                })
                        , Integer.MAX_VALUE)
                .onErrorContinue((err, obj) -> log.error(err.getMessage(), err))
                .contextWrite(ReactiveLogger.start("network", tcpServer.getId()))
                .subscribe(
                        ignore -> {
                        },
                        error -> log.error(error.getMessage(), error)
                );
    }

    @Override
    protected Mono<Void> doShutdown() {
        if (disposable != null) {
            disposable.dispose();
        }
        return Mono.empty();
    }

    @Override
    protected Mono<Void> doStartup() {
        return Mono.fromRunnable(this::doStart);
    }
}