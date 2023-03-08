package top.iot.gateway.network.mqtt.gateway.device;

import com.alibaba.fastjson.JSON;
import top.iot.gateway.component.elasticsearch.index.ElasticIndex;
import top.iot.gateway.component.elasticsearch.index.EsSourceData;
import top.iot.gateway.component.elasticsearch.service.ElasticSearchService;
import top.iot.gateway.component.gateway.AbstractDeviceGateway;
import top.iot.gateway.network.DefaultNetworkType;
import top.iot.gateway.network.DeviceMessageIndex;
import top.iot.gateway.network.NetworkType;
import top.iot.gateway.network.mq.devtype.IotDevTypeProducer;
import top.iot.gateway.network.mqtt.client.MqttClient;
import top.iot.gateway.network.mqtt.gateway.device.session.MqttClientSession;
import top.iot.gateway.network.mqtt.gateway.device.session.UnknownDeviceMqttClientSession;
import top.iot.gateway.network.utils.CustomTextMessageParser;
import top.iot.gateway.network.utils.DeviceGatewayHelper;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import top.iot.gateway.core.ProtocolSupport;
import top.iot.gateway.core.ProtocolSupports;
import top.iot.gateway.core.device.DeviceOperator;
import top.iot.gateway.core.device.DeviceRegistry;
import top.iot.gateway.core.device.session.DeviceSessionManager;
import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.message.codec.*;
import top.iot.gateway.core.server.session.DeviceSession;
import top.iot.gateway.supports.server.DecodedClientMessageHandler;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class MqttClientDeviceGateway extends AbstractDeviceGateway {

    private final MqttClient mqttClient;

    private final DeviceRegistry registry;

    private final List<String> topics;

    private final int qos;

    private final String protocol;

    private final ProtocolSupports protocolSupport;

    private Disposable disposable = null;

    private final DeviceGatewayHelper helper;

    private final ElasticSearchService elasticSearchService;

    private final String deviceCode;

    private IotDevTypeProducer iotDevTypeProducer;

    public MqttClientDeviceGateway(String id,
                                   MqttClient mqttClient,
                                   DeviceRegistry registry,
                                   ProtocolSupports protocolSupport,
                                   String protocol,
                                   DeviceSessionManager sessionManager,
                                   DecodedClientMessageHandler clientMessageHandler,
                                   List<String> topics,
                                   int qos,
                                   ElasticSearchService elasticSearchService,
                                   String deviceCode,
                                   IotDevTypeProducer iotDevTypeProducer) {
        super(id);
        this.mqttClient = Objects.requireNonNull(mqttClient, "mqttClient");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.protocolSupport = Objects.requireNonNull(protocolSupport, "protocolSupport");
        this.protocol = Objects.requireNonNull(protocol, "protocol");
        this.topics = Objects.requireNonNull(topics, "topics");
        this.helper = new DeviceGatewayHelper(registry, sessionManager, clientMessageHandler);
        this.qos = qos;
        this.elasticSearchService = elasticSearchService;
        this.deviceCode = deviceCode;
        this.iotDevTypeProducer = iotDevTypeProducer;
    }


    protected Mono<ProtocolSupport> getProtocol() {
        return protocolSupport.getProtocol(protocol);
    }

    private void doStart() {
        if (disposable != null) {
            disposable.dispose();
        }
        disposable = mqttClient
                .subscribe(topics, qos)
                .filter((msg) -> isStarted())
                .flatMap(mqttMessage -> {
                    AtomicReference<Duration> timeoutRef = new AtomicReference<>();
                    SimpleMqttMessage simpleMqttMessage = simpleMqttMessage(mqttMessage.getPayload().toString(StandardCharsets.UTF_8));
                    simpleMqttMessage.setTopic(mqttMessage.getTopic());
                    String deviceId = getDeviceId(simpleMqttMessage, deviceCode);
                    log.info("mqtt订阅设备编号:{}的设备", deviceId);
                    // 修改es索引  DeviceMessageIndexProvider.DEVICE_ORIGINAL_MESSAGE
                    String logType = "metric";
                    String type = mqttMessage.getTopic().split("/")[mqttMessage.getTopic().split("/").length - 1];
                    //todo 根据type区分logType
                    ElasticIndex elasticIndex = new DeviceMessageIndex(logType, System.currentTimeMillis());
                    return elasticSearchService.commit(elasticIndex, Mono.just(new EsSourceData(deviceId, type, simpleMqttMessage.getPayload().toString(StandardCharsets.UTF_8))))
                            .onErrorContinue((err, obj) -> log.error("es处理失败", err))
                            .doOnSuccess(__ -> {
                                getProtocol()
                                        .flatMap(codec -> codec.getMessageCodec(getTransport()))
                                        .flatMapMany(codec -> codec.decode(new FromDeviceMessageContext() {
                                            @Nonnull
                                            @Override
                                            public EncodedMessage getMessage() {
                                                return simpleMqttMessage;
                                            }

                                            @Override
                                            public DeviceSession getSession() {
                                                return new UnknownDeviceMqttClientSession(getId() + ":unknown", mqttClient) {
                                                    @Override
                                                    public Mono<Boolean> send(EncodedMessage encodedMessage) {
                                                        return super.send(encodedMessage).doOnSuccess(r -> monitor.sentMessage());
                                                    }

                                                    @Override
                                                    public void setKeepAliveTimeout(Duration timeout) {
                                                        timeoutRef.set(timeout);
                                                    }
                                                };
                                            }

                                            @Override
                                            public Mono<DeviceOperator> getDeviceAsync() {
                                                return registry.getDevice(deviceId).switchIfEmpty(Mono.fromRunnable(() -> log.warn("device [{}] not fond in registry", deviceId)));
                                            }
                                        }))
                                        .doOnError((err) -> log.error("解码MQTT客户端消息失败 {}:{}",
                                                mqttMessage.getTopic(),
                                                mqttMessage
                                                        .getPayload()
                                                        .toString(StandardCharsets.UTF_8),
                                                err))
                                        .cast(DeviceMessage.class)
                                        .flatMap(message -> {
                                            monitor.receivedMessage();
                                            return helper
                                                    .handleDeviceMessage(message,
                                                            device -> createDeviceSession(device, mqttClient),
                                                            DeviceGatewayHelper.applySessionKeepaliveTimeout(message, timeoutRef::get),
                                                            () -> log.warn("无法从MQTT[{}]消息中获取设备信息:{}", mqttMessage.print(), message)
                                                    ).thenReturn(message);
                                        })
                                        .flatMap(msg -> {
                                            log.info("MQTTClient 发送消息给下游：{}", JSON.toJSONString(msg));
                                            return iotDevTypeProducer.sendMessage(msg).thenReturn(msg);
                                        })
                                        .then()
                                        .onErrorResume((err) -> {
                                            log.error("处理MQTT消息失败:{}", mqttMessage, err);
                                            return Mono.empty();
                                        }).subscribe();

                            });
                }, Integer.MAX_VALUE)
                .onErrorContinue((err, ms) -> log.error("处理MQTT客户端消息失败", err))
                .subscribe();

    }

    private SimpleMqttMessage simpleMqttMessage(String str) {
        SimpleMqttMessage mqttMessage = new SimpleMqttMessage();
        CustomTextMessageParser.of(
                start -> {
                    //QoS0 /topic
                    String[] qosAndTopic = start.split("[ ]");
                    if (qosAndTopic.length == 1) {
                        mqttMessage.setTopic(qosAndTopic[0]);
                    } else {
                        mqttMessage.setTopic(qosAndTopic[1]);
                        String qos = qosAndTopic[0].toLowerCase();
                        if (qos.length() == 1) {
                            mqttMessage.setQosLevel(Integer.parseInt(qos));
                        } else {
                            mqttMessage.setQosLevel(Integer.parseInt(qos.substring(qos.length() - 1)));
                        }
                    }
                },
                (header, value) -> {

                },
                body -> {
                    mqttMessage.setPayload(Unpooled.wrappedBuffer(body.getBody()));
                    mqttMessage.setPayloadType(body.getType());
                },
                () -> mqttMessage.setPayload(Unpooled.wrappedBuffer(new byte[0]))
        ).parse(str);

        return mqttMessage;
    }

    private MqttClientSession createDeviceSession(DeviceOperator device, MqttClient client) {
        Mono.defer(() -> {
            Map<String, Object> configs = new HashMap<>();
            configs.put("protocol", protocol);
            configs.put("transport", getTransport().getId());
            return Mono.just(configs);
        }).flatMap(device::setConfigs).subscribe();
        return new MqttClientSession(device.getDeviceId(), device, client, monitor);
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.MQTT;
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.MQTT_CLIENT;
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
