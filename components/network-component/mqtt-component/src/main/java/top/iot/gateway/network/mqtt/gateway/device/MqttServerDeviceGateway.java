package top.iot.gateway.network.mqtt.gateway.device;

import com.alibaba.fastjson.JSON;
import top.iot.gateway.component.common.utils.SystemUtils;
import top.iot.gateway.component.elasticsearch.index.ElasticIndex;
import top.iot.gateway.component.elasticsearch.index.EsSourceData;
import top.iot.gateway.component.elasticsearch.service.ElasticSearchService;
import top.iot.gateway.component.gateway.AbstractDeviceGateway;
import top.iot.gateway.component.gateway.monitor.MonitorSupportDeviceGateway;
import top.iot.gateway.network.DefaultNetworkType;
import top.iot.gateway.network.DeviceMessageIndex;
import top.iot.gateway.network.NetworkType;
import top.iot.gateway.network.mq.devtype.IotDevTypeProducer;
import top.iot.gateway.network.mqtt.gateway.device.session.MqttConnectionSession;
import top.iot.gateway.network.mqtt.server.MqttConnection;
import top.iot.gateway.network.mqtt.server.MqttServer;
import top.iot.gateway.network.utils.DeviceGatewayHelper;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import top.iot.gateway.core.ProtocolSupport;
import top.iot.gateway.core.device.*;
import top.iot.gateway.core.device.session.DeviceSessionManager;
import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.message.codec.DefaultTransport;
import top.iot.gateway.core.message.codec.FromDeviceMessageContext;
import top.iot.gateway.core.message.codec.MqttMessage;
import top.iot.gateway.core.message.codec.Transport;
import top.iot.gateway.core.server.session.DeviceSession;
import top.iot.gateway.core.server.session.KeepOnlineSession;
import top.iot.gateway.core.trace.DeviceTracer;
import top.iot.gateway.core.trace.FluxTracer;
import top.iot.gateway.supports.server.DecodedClientMessageHandler;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

@Slf4j
class MqttServerDeviceGateway extends AbstractDeviceGateway implements MonitorSupportDeviceGateway {

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final MqttServer mqttServer;

    private final DecodedClientMessageHandler messageHandler;

    private final LongAdder counter = new LongAdder();

    private final Mono<ProtocolSupport> supportMono;

    private Disposable disposable;

    private final DeviceGatewayHelper helper;

    private final ElasticSearchService elasticSearchService;

    private IotDevTypeProducer iotDevTypeProducer;

    public MqttServerDeviceGateway(String id,
                                   DeviceRegistry registry,
                                   DeviceSessionManager sessionManager,
                                   MqttServer mqttServer,
                                   DecodedClientMessageHandler messageHandler,
                                   Mono<ProtocolSupport> customProtocol,
                                   ElasticSearchService elasticSearchService,
                                   IotDevTypeProducer iotDevTypeProducer) {
        super(id);
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.mqttServer = mqttServer;
        this.messageHandler = messageHandler;
        this.supportMono = customProtocol;
        this.helper = new DeviceGatewayHelper(registry, sessionManager, messageHandler);
        this.elasticSearchService = elasticSearchService;
        this.iotDevTypeProducer = iotDevTypeProducer;
    }

    @Override
    public long totalConnection() {
        return counter.sum();
    }

    private void doStart() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = mqttServer
                .handleConnection()
                .filter(conn -> {
                    if (!isStarted()) {
                        conn.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
                        monitor.rejected();
                    }
                    return isStarted();
                })
                .publishOn(Schedulers.parallel())
                .flatMap(this::handleConnection)
                .flatMap(tuple3 -> handleAuthResponse(tuple3.getT1(), tuple3.getT2(), tuple3.getT3()))
                .flatMap(tp -> handleAcceptedMqttConnection(tp.getT1(), tp.getT2(), tp.getT3()), Integer.MAX_VALUE)
                .onErrorContinue((err, obj) -> log.error("处理MQTT连接失败", err))
                .contextWrite(ReactiveLogger.start("network", mqttServer.getId()))
                .subscribe();

    }

    //处理连接，并进行认证
    private Mono<Tuple3<DeviceOperator, AuthenticationResponse, MqttConnection>> handleConnection(MqttConnection connection) {
        //内存不够了
        if (SystemUtils.memoryIsOutOfWatermark()) {
            //直接拒绝,响应SERVER_UNAVAILABLE,不再处理此连接
            connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
            return Mono.empty();
        }
        return Mono
                .justOrEmpty(connection.getAuth())
                .flatMap(auth -> {
                    MqttAuthenticationRequest request = new MqttAuthenticationRequest(connection.getClientId(), auth.getUsername(), auth
                            .getPassword(), getTransport());
                    return supportMono
                            //使用自定义协议来认证
                            .map(support -> support.authenticate(request, registry))
                            .defaultIfEmpty(Mono.defer(() -> registry
                                    .getDevice(connection.getClientId())
                                    .flatMap(device -> device.authenticate(request))))
                            .flatMap(Function.identity())
                            .switchIfEmpty(Mono.fromRunnable(() -> connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD)));
                })
                .flatMap(resp -> {
                    String deviceId = StringUtils.isEmpty(resp.getDeviceId()) ? connection.getClientId() : resp.getDeviceId();
                    //认证返回了新的设备ID,则使用新的设备
                    Mono<DeviceOperator> deviceOperator = registry.getDevice(deviceId);
                    return deviceOperator
                            .map(operator -> Tuples.of(operator, resp, connection))
                            .switchIfEmpty(Mono.fromRunnable(() -> connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED)))
                            ;
                })
                //设备注册信息不存在,拒绝连接
                .onErrorResume((err) -> Mono.fromRunnable(() -> {
                    log.error("MQTT连接认证[{}]失败", connection.getClientId(), err);
                    //监控信息
                    monitor.rejected();
                    //应答SERVER_UNAVAILABLE
                    connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
                }));
    }

    //处理认证结果
    private Mono<Tuple3<MqttConnection, DeviceOperator, MqttConnectionSession>> handleAuthResponse(DeviceOperator device,
                                                                                                   AuthenticationResponse resp,
                                                                                                   MqttConnection connection) {
        return Mono
                .defer(() -> {
                    String deviceId = device.getDeviceId();
                    if (resp.isSuccess()) {
                        //监听断开连接
                        connection.onClose(conn -> {
                            counter.decrement();
                            //监控信息
                            monitor.disconnected();
                            monitor.totalConnection(counter.sum());

                            sessionManager
                                    .getSession(deviceId)
                                    .flatMap(_tmp -> {
                                        //只有与创建的会话相同才移除(下线),因为有可能设置了keepOnline,
                                        //或者设备通过其他方式注册了会话,这里断开连接不能影响到以上情况.
                                        if (_tmp != null && _tmp.isWrapFrom(MqttConnectionSession.class) && !(_tmp instanceof KeepOnlineSession)) {
                                            MqttConnectionSession connectionSession = _tmp.unwrap(MqttConnectionSession.class);
                                            if (connectionSession.getConnection() == conn) {
                                                log.info("[{}]移除会话", deviceId);
                                                return sessionManager.remove(deviceId, true);
                                            }
                                        }
                                        return Mono.empty();
                                    })
                                    .subscribe();
                        });

                        counter.increment();
                        log.info("[{}]更新会话", deviceId);
                        return sessionManager.compute(deviceId, old -> {
                            MqttConnectionSession newSession = new MqttConnectionSession(deviceId, device, getTransport(), connection, monitor);
                            return old
                                    .<DeviceSession>map(session -> {
                                        if (session instanceof KeepOnlineSession) {
                                            //KeepOnlineSession 则依旧保持keepOnline
                                            return new KeepOnlineSession(newSession, session.getKeepAliveTimeout());
                                        }
                                        return newSession;
                                    })
                                    .defaultIfEmpty(newSession);
                        })
                                .flatMap(session -> Mono.fromCallable(() -> {
                                    try {
                                        return Tuples.of(connection.accept(), device, session.unwrap(MqttConnectionSession.class));
                                    } catch (IllegalStateException ignore) {
                                        //忽略错误,偶尔可能会出现网络异常,导致accept时,连接已经中断.还有其他更好的处理方式?
                                        return null;
                                    }
                                }))
                                .doOnNext(o -> {
                                    //监控信息
                                    monitor.connected();
                                    monitor.totalConnection(counter.sum());
                                });
                    } else {
                        //认证失败返回 0x04 BAD_USER_NAME_OR_PASSWORD
                        connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD);
                        monitor.rejected();
                        log.warn("MQTT客户端认证[{}]失败:{}", deviceId, resp.getMessage());
                    }
                    return Mono.empty();
                })
                .onErrorResume(error -> Mono.fromRunnable(() -> {
                    log.error(error.getMessage(), error);
                    monitor.rejected();
                    connection.reject(MqttConnectReturnCode.CONNECTION_REFUSED_SERVER_UNAVAILABLE);
                }));
    }

    //处理已经建立连接的MQTT连接
    private Mono<Void> handleAcceptedMqttConnection(MqttConnection connection, DeviceOperator operator, MqttConnectionSession session) {

        return Flux
                .usingWhen(Mono.just(connection),
                        MqttConnection::handleMessage,
                        MqttConnection::close)
                //网关暂停或者已停止时,则不处理消息
                .filter(pb -> isStarted())
                .doOnNext(msg -> monitor.receivedMessage())
                .flatMap(publishing ->
                        this.decodeAndHandleMessage(operator, session, publishing.getMessage(), connection)
                                //ack
                                .doOnSuccess(s -> publishing.acknowledge())
                )
                //合并遗言消息
                .mergeWith(
                        Mono.justOrEmpty(connection.getWillMessage())
                                .flatMap(mqttMessage -> this.decodeAndHandleMessage(operator, session, mqttMessage, connection))
                )
                .then();
    }

    //解码消息并处理
    private Mono<Void> decodeAndHandleMessage(DeviceOperator operator,
                                              MqttConnectionSession session,
                                              MqttMessage message,
                                              MqttConnection connection) {
        log.info("mqtt订阅设备编号:{}的设备", operator.getDeviceId());
        // 修改es索引  DeviceMessageIndexProvider.DEVICE_ORIGINAL_MESSAGE
        String logType = "metric";
        String type = message.getTopic().split("/")[message.getTopic().split("/").length - 1];
        //todo 根据type区分logType
        ElasticIndex elasticIndex = new DeviceMessageIndex(logType, System.currentTimeMillis());
        String source = message.getPayload().toString(StandardCharsets.UTF_8);
        return elasticSearchService.commit(elasticIndex, Mono.just(new EsSourceData(operator.getDeviceId(), type, source)))
                .onErrorContinue((err, obj) -> log.error("es处理失败", err))
                .then(
                        operator.getProtocol()
                        .flatMap(protocol -> {
                            Transport transport = getTransport();
                            return protocol.getMessageCodec(transport);
                        })
                        .flatMapMany(codec -> codec.decode(FromDeviceMessageContext.of(session, message, registry)))
                        .doOnError((err) -> log.error("解码MQTT服务端消息失败 {}:{}",
                                message.getTopic(),
                                source,
                                err))
                        .cast(DeviceMessage.class)
                        .flatMap(msg -> {
                            //回填deviceId,有的场景协议包不能或者没有解析出deviceId,则直接使用连接对应的设备id进行填充.
                            if (!StringUtils.hasText(msg.getDeviceId())) {
                                msg.thingId(DeviceThingType.device, operator.getDeviceId());
                            }
                            return this.handleMessage(operator, msg, connection);
                        })
                        .flatMap(msg -> {
                            log.info("MQTTServer 发送消息给下游：{}", JSON.toJSONString(msg));
                            return iotDevTypeProducer.sendMessage(msg).thenReturn(msg);
                        })
                        .doOnEach(ReactiveLogger.onError(err -> log.error("处理MQTT连接[{}]消息失败:{}", operator.getDeviceId(), message, err)))
                        .as(FluxTracer.create(DeviceTracer.SpanName.decode(operator.getDeviceId()),
                                        (span, msg) -> span.setAttribute(DeviceTracer.SpanKey.message, msg
                                                .toJson()
                                                .toJSONString())))
                        //发生错误不中断流
                        .onErrorResume((err) -> {
                            log.error("处理MQTT消息失败:{}", message);
                            return Mono.empty();
                        })
                        .then()
                        .subscribeOn(Schedulers.parallel())
                );
    }

    protected Mono<ProtocolSupport> getProtocol() {
        return supportMono;
    }

    private Mono<DeviceMessage> handleMessage(DeviceOperator mainDevice,
                                              DeviceMessage message,
                                              MqttConnection connection) {
        if (!connection.isAlive()) {
            return messageHandler
                    .handleMessage(mainDevice, message)
                    .thenReturn(message);
        }
        return helper.handleDeviceMessage(message,
                device -> {
                    getProtocol().map(protocolSupport -> {
                        Map<String, Object> configs = new HashMap<>();
                        configs.put("protocol", protocolSupport.getId());
                        configs.put("transport", getTransport().getId());
                        return configs;
                    }).flatMap(device::setConfigs).subscribe();
                    return new MqttConnectionSession(device.getDeviceId(),
                            device,
                            getTransport(),
                            connection,
                            monitor);
                },
                session -> {

                },
                () -> {
                    log.warn("无法从MQTT[{}]消息中获取设备信息:{}", connection.getClientId(), message);
                })
                .thenReturn(message);
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.MQTT;
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.MQTT_SERVER;
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
