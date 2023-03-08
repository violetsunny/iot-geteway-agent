package top.iot.gateway.network.udp.device;

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
import top.iot.gateway.network.SessionIdDeviceIdBindingRegistry;
import top.iot.gateway.network.mq.devtype.IotDevTypeProducer;
import top.iot.gateway.network.udp.client.UdpClient;
import top.iot.gateway.network.udp.message.UdpMessage;
import top.iot.gateway.network.utils.DeviceGatewayHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.logger.ReactiveLogger;
import top.iot.gateway.core.ProtocolSupport;
import top.iot.gateway.core.ProtocolSupports;
import top.iot.gateway.core.Value;
import top.iot.gateway.core.device.DeviceOperator;
import top.iot.gateway.core.device.DeviceRegistry;
import top.iot.gateway.core.device.session.DeviceSessionManager;
import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.codec.DefaultTransport;
import top.iot.gateway.core.message.codec.EncodedMessage;
import top.iot.gateway.core.message.codec.FromDeviceMessageContext;
import top.iot.gateway.core.message.codec.Transport;
import top.iot.gateway.core.server.session.DeviceSession;
import top.iot.gateway.supports.server.DecodedClientMessageHandler;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

@Slf4j(topic = "system.udp.gateway")
public class UdpDeviceGateway implements DeviceGateway, MonitorSupportDeviceGateway {

    @Getter
    private final String id;

    private final UdpClient udpClient;

    private final String protocol;

    private final ProtocolSupports supports;

    private final DeviceRegistry registry;

    private final DeviceGatewayMonitor gatewayMonitor;

    /**
     * 连接计数器
     */
    private final LongAdder counter = new LongAdder();

    private final EmitterProcessor<Message> processor = EmitterProcessor.create(false);

    private final FluxSink<Message> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final AtomicBoolean started = new AtomicBoolean();
    private final DeviceGatewayHelper helper;
    /**
     * 数据流控开关
     */
    private Disposable disposable;

    private final String deviceCode;

    private final SessionIdDeviceIdBindingRegistry sessionIdDeviceIdBindingRegistry;

    private IotDevTypeProducer iotDevTypeProducer;

    private final ElasticSearchService elasticSearchService;

    public UdpDeviceGateway(String id,
                            String protocol,
                            ProtocolSupports supports,
                            DeviceRegistry deviceRegistry,
                            DecodedClientMessageHandler clientMessageHandler,
                            DeviceSessionManager sessionManager,
                            UdpClient udpClient,
                            ElasticSearchService elasticSearchService,
                            String deviceCode,
                            SessionIdDeviceIdBindingRegistry sessionIdDeviceIdBindingRegistry,
                            IotDevTypeProducer iotDevTypeProducer) {
        this.gatewayMonitor = GatewayMonitors.getDeviceGatewayMonitor(id);
        this.id = id;
        this.protocol = protocol;
        this.registry = deviceRegistry;
        this.supports = supports;
        this.udpClient = udpClient;
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
        return DefaultTransport.UDP;
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.UDP;
    }

    private void doStart() {
        if (started.getAndSet(true) || disposable != null) {
            return;
        }
        disposable = udpClient
                .subscribe()
                .publishOn(Schedulers.parallel())
                .flatMap(this::handleUdpMessage)
                .onErrorResume((err) -> {
                    log.error(err.getMessage(), err);
                    udpClient.shutdown();
                    return Mono.empty();
                })
                .then()
                .doOnCancel(udpClient::shutdown)
                .onErrorContinue((err, obj) -> log.error(err.getMessage(), err))
                .subscriberContext(ReactiveLogger.start("network", udpClient.getId()))
                .subscribe(
                        ignore -> {
                        },
                        error -> log.error(error.getMessage(), error)
                );
    }

    private Mono<Void> handleUdpMessage(UdpMessage message) {
        String payload = message.payloadAsString();
        if ("X".equals(payload)) return Mono.empty();
        log.info("udp原始消息为：：{}", message);

        EncodedMessage udpMessage = EncodedMessage.simple(message.getPayload());
        if (StringUtils.hasText(deviceCode)) {
            return sessionIdDeviceIdBindingRegistry.getConfig(udpClient.getId(), false)
                    .map(Value::asString)
                    .switchIfEmpty(Mono.defer(() -> {
                        String deviceId = getDeviceId(udpMessage, deviceCode);
                        return sessionIdDeviceIdBindingRegistry.setConfig(udpClient.getId(), deviceId).thenReturn(deviceId);
                    }))
                    .flatMap(deviceId -> {
                                String logType = "metric";
                                ElasticIndex elasticIndex = new DeviceMessageIndex(logType, System.currentTimeMillis());
                                // Mono.just(new EsSourceData(deviceId, "rtg", Hex.encodeHexString(message.payloadAsBytes())))
                                return elasticSearchService.commit(elasticIndex, Mono.just(new EsSourceData(deviceId, "rtg", Hex.encodeHexString(message.payloadAsBytes()))))
                                        .onErrorContinue((err, obj) -> log.error("es处理失败", err))
                                        .then(
                                                getProtocol()
                                                        .flatMap(pt -> pt.getMessageCodec(getTransport()))
                                                        .flatMapMany(codec -> codec.decode(new FromDeviceMessageContext() {
                                                            @Override
                                                            public DeviceSession getSession() {
                                                                return new UnknownUdpDeviceSession(udpClient.getId(), udpClient, DefaultTransport.UDP);
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
                                                        .doOnNext(msg -> gatewayMonitor.receivedMessage())
                                                        .flatMap(this::handleDeviceMessage)
                                                        .flatMap(msg -> {
                                                            log.info("Udp 发送消息给下游：{}", JSON.toJSONString(msg));
                                                            return iotDevTypeProducer.sendMessage(msg).thenReturn(msg);
                                                        })
                                                        .doOnEach(ReactiveLogger.onError(err -> log.error("UDP[{}]消息失败:\n{}",
                                                                message
                                                                , err)))
                                                        .onErrorResume((err) -> Mono.fromRunnable(udpClient::reset))
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
                    .flatMapMany(codec -> codec.decode(FromDeviceMessageContext.of(new UnknownUdpDeviceSession(udpClient.getId(), udpClient, DefaultTransport.UDP), message, registry)))
                    .cast(DeviceMessage.class)
                    .doOnNext(msg -> gatewayMonitor.receivedMessage())
                    .flatMap(this::handleDeviceMessage)
                    .flatMap(msg -> {
                        log.info("Udp 发送消息给下游：{}", JSON.toJSONString(msg));
                        return iotDevTypeProducer.sendMessage(msg).thenReturn(msg);
                    })
                    .doOnEach(ReactiveLogger.onError(err -> log.error("UDP[{}]消息失败:\n{}",
                            message
                            , err)))
                    .onErrorResume((err) -> Mono.fromRunnable(udpClient::reset))
                    .flatMap(msg -> {
                        String logType = "metric";
                        ElasticIndex elasticIndex = new DeviceMessageIndex(logType, System.currentTimeMillis());
                        return elasticSearchService.commit(elasticIndex, Mono.just(new EsSourceData(msg.getDeviceId(), "rtg", Hex.encodeHexString(message.payloadAsBytes()))))
                                .onErrorContinue((err, obj) -> log.error("es处理失败", err))
                                .thenReturn(msg);
                    })
                    .then();
        }

    }

    private Mono<DeviceMessage> handleDeviceMessage(DeviceMessage message) {
        log.info("最终的消息为:{}", message);
        return helper
                .handleDeviceMessage(message,
                        device -> new UdpDeviceSession(device, udpClient, DefaultTransport.UDP),
                        session -> {
                            UdpDeviceSession deviceSession = session.unwrap(UdpDeviceSession.class);
                            deviceSession.setClient(udpClient);

                        },
                        () -> log.warn("无法获取设备udp信息:{}", message)
                ).thenReturn(message);
    }

    @Override
    public Flux<Message> onMessage() {
        return processor;
    }

    @Override
    public Mono<Void> pause() {
        return Mono.fromRunnable(() -> started.set(false));
    }

    @Override
    public Mono<Void> startup() {
        return Mono.fromRunnable(this::doStart);
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
}