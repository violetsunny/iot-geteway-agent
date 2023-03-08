package top.iot.gateway.network.coap.device;

import com.alibaba.fastjson.JSON;
import top.iot.gateway.component.gateway.DeviceGateway;
import top.iot.gateway.component.gateway.monitor.DeviceGatewayMonitor;
import top.iot.gateway.component.gateway.monitor.GatewayMonitors;
import top.iot.gateway.component.gateway.monitor.MonitorSupportDeviceGateway;
import top.iot.gateway.network.DefaultNetworkType;
import top.iot.gateway.network.NetworkType;
import top.iot.gateway.network.coap.server.COAPServer;
import top.iot.gateway.network.coap.server.CoapConnection;
import top.iot.gateway.network.mq.devtype.IotDevTypeProducer;
import top.iot.gateway.network.utils.DeviceGatewayHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import top.iot.gateway.core.ProtocolSupport;
import top.iot.gateway.core.device.DeviceOperator;
import top.iot.gateway.core.device.DeviceRegistry;
import top.iot.gateway.core.device.session.DeviceSessionManager;
import top.iot.gateway.core.message.CommonDeviceMessage;
import top.iot.gateway.core.message.CommonDeviceMessageReply;
import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.codec.*;
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

@Slf4j
public class CoapServerDeviceGateway implements DeviceGateway, MonitorSupportDeviceGateway {
    @Getter
    private final String id;

    private final DeviceRegistry registry;

    private final COAPServer coapServer;

    private final DeviceGatewayMonitor gatewayMonitor;

    private final LongAdder counter = new LongAdder();

    private final EmitterProcessor<Message> messageProcessor = EmitterProcessor.create(false);

    private final FluxSink<Message> sink = messageProcessor.sink(FluxSink.OverflowStrategy.BUFFER);

    private final AtomicBoolean started = new AtomicBoolean();

    private final Mono<ProtocolSupport> supportMono;

    private Disposable disposable;

    private final DeviceGatewayHelper helper;

    private IotDevTypeProducer iotDevTypeProducer;

    public CoapServerDeviceGateway(String id,
                                   DeviceRegistry registry,
                                   DeviceSessionManager sessionManager,
                                   COAPServer coapServer,
                                   DecodedClientMessageHandler messageHandler,
                                   Mono<ProtocolSupport> customProtocol,
                                   IotDevTypeProducer iotDevTypeProducer) {
        this.gatewayMonitor = GatewayMonitors.getDeviceGatewayMonitor(id);
        this.id = id;
        this.registry = registry;
        this.coapServer = coapServer;
        this.supportMono = customProtocol;
        this.helper = new DeviceGatewayHelper(registry, sessionManager, messageHandler);
        this.iotDevTypeProducer = iotDevTypeProducer;
    }

    @Override
    public long totalConnection() {
        return counter.sum();
    }

    private void doStart() {
        if (started.getAndSet(true) || disposable != null) {
            return;
        }
        disposable = coapServer
            .handleConnection()
            .filter(conn -> {
                if (!started.get()) {
                    gatewayMonitor.rejected();
                    conn.reject();
                }
                return started.get();
            })
            .publishOn(Schedulers.parallel())
            .flatMap(this::handleConnection, Integer.MAX_VALUE)
            .flatMap(this::handleAcceptedConnection, Integer.MAX_VALUE)
            .onErrorContinue((err, obj) -> log.error("处理COAP连接失败", err))
            .subscriberContext(ReactiveLogger.start("network", coapServer.getId()))
            .subscribe();
    }

    private Mono<CoapConnection> handleConnection(CoapConnection connection) {
        return Mono.fromCallable(() -> {
            counter.increment();
            gatewayMonitor.totalConnection(counter.sum());
            gatewayMonitor.connected();

            connection.onClose(conn -> {
                counter.decrement();
                gatewayMonitor.disconnected();
                gatewayMonitor.totalConnection(counter.sum());
            });
            return connection;
//            DeviceSession session = sessionManager.getSession(connection.getClientId());
//            if (session == null) {
//                session = new CoapDeviceSession(null, connection, getTransport(), gatewayMonitor) {
//                    @Override
//                    public Mono<Boolean> send(EncodedMessage encodedMessage) {
//                        return super.send(encodedMessage).doOnSuccess(r -> gatewayMonitor.sentMessage());
//                    }
//                };
//            }
//            return Tuples.of(connection, session);
        });
    }

    private Mono<Void> handleAcceptedConnection(CoapConnection connection) {
        return connection
            .handleMessage()
            .filter(pb -> started.get())
            .doOnCancel(() -> {
                //流被取消时(可能网关关闭了)断开连接
                connection.close().subscribe();
            })
            .publishOn(Schedulers.parallel())
            .doOnNext(msg -> gatewayMonitor.receivedMessage())
            .flatMap(msg -> decodeAndHandleMessage(msg, connection))
            .subscriberContext(ReactiveLogger.start("network", coapServer.getId()))
            .then();
    }

    //解码消息并处理
    private Mono<Void> decodeAndHandleMessage(CoapMessage message,
                                              CoapConnection connection) {
        return supportMono
            .flatMap(protocol -> protocol.getMessageCodec(getTransport()))
            .flatMapMany(codec -> codec.decode(new FromDeviceMessageContext() {
                @Nonnull
                @Override
                public EncodedMessage getMessage() {
                    return message;
                }

                @Override
                public DeviceSession getSession() {
                    return new CoapDeviceSession(null, connection, getTransport(), gatewayMonitor);
                }

                @Override
                public Mono<DeviceOperator> getDeviceAsync() {
//                    String deviceId = getDeviceId(getMessage(), deviceCode);
//                    String deviceId = "testcoap001";
                    String deviceId = message.payloadAsJson().getString("deviceId");
                    log.info("coap订阅设备编号:{}的设备", deviceId);
                    return registry.getDevice(deviceId).switchIfEmpty(Mono.fromRunnable(() -> log.warn("device [{}] not fond in registry", deviceId)));
                }
            }))
            .cast(DeviceMessage.class)
            .flatMap(msg -> {
                String deviceId = msg.getDeviceId();
                if (messageProcessor.hasDownstreams()) {
                    sink.next(msg);
                }
                if (msg instanceof CommonDeviceMessage) {
                    CommonDeviceMessage _msg = ((CommonDeviceMessage) msg);
                    if (StringUtils.isEmpty(_msg.getDeviceId())) {
                        _msg.setDeviceId(deviceId);
                    }
                }
                if (msg instanceof CommonDeviceMessageReply) {
                    CommonDeviceMessageReply<?> _msg = ((CommonDeviceMessageReply<?>) msg);
                    if (StringUtils.isEmpty(_msg.getDeviceId())) {
                        _msg.setDeviceId(deviceId);
                    }
                }
                return handleDeviceMessage(msg, connection);
            })
            .flatMap(msg -> {
                log.info("CoapServer 发送消息给下游：{}", JSON.toJSONString(msg));
                return iotDevTypeProducer.sendMessage(msg).thenReturn(msg);
            })
            .then()
            .doOnEach(ReactiveLogger.onError(err -> log.error("处理COAP连接[{}]消息失败:{}", connection.getClientId(), message, err)))
            .onErrorResume((err) -> Mono.empty())//发生错误不中断流
            ;
    }

    Mono<DeviceMessage> handleDeviceMessage(DeviceMessage message, CoapConnection connection) {
        if (messageProcessor.hasDownstreams()) {
            sink.next(message);
        }
        return helper
            .handleDeviceMessage(message,
                device -> new CoapDeviceSession(device, connection, getTransport(), gatewayMonitor),
                session -> {

                },
                () -> log.warn("无法从coap[{}]消息中获取设备信息:{}", connection.getClientId(), message)
            )
            .thenReturn(message);
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.CoAP;
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.COAP_SERVER;
    }

    @Override
    public Flux<Message> onMessage() {
        return messageProcessor;
    }

    @Override
    public Mono<Void> startup() {
        return Mono.fromRunnable(this::doStart);
    }

    @Override
    public Mono<Void> pause() {
        return Mono.fromRunnable(() -> started.set(false));
    }

    @Override
    public Mono<Void> shutdown() {
        return Mono.fromRunnable(() -> {
            started.set(false);
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
            disposable = null;
        });
    }
}
