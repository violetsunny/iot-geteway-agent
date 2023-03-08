package top.iot.gateway.network.mqtt.gateway.device;

import top.iot.gateway.component.gateway.DeviceGateway;
import top.iot.gateway.component.gateway.supports.DeviceGatewayProperties;
import top.iot.gateway.component.gateway.supports.DeviceGatewayProvider;
import top.iot.gateway.component.elasticsearch.service.ElasticSearchService;
import top.iot.gateway.network.mq.devtype.IotDevTypeProducer;
import top.iot.gateway.network.mqtt.server.MqttServer;
import top.iot.gateway.network.DefaultNetworkType;
import top.iot.gateway.network.NetworkManager;
import top.iot.gateway.network.NetworkType;
import top.iot.gateway.core.ProtocolSupports;
import top.iot.gateway.core.device.DeviceRegistry;
import top.iot.gateway.core.device.session.DeviceSessionManager;
import top.iot.gateway.supports.server.DecodedClientMessageHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

@Component
public class MqttServerDeviceGatewayProvider implements DeviceGatewayProvider {

    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler messageHandler;

    private final ProtocolSupports protocolSupports;

    private final ElasticSearchService elasticSearchService;

    public MqttServerDeviceGatewayProvider(NetworkManager networkManager,
                                           DeviceRegistry registry,
                                           DeviceSessionManager sessionManager,
                                           DecodedClientMessageHandler messageHandler,
                                           ProtocolSupports protocolSupports,
                                           ElasticSearchService elasticSearchService) {
        this.networkManager = networkManager;
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.messageHandler = messageHandler;
        this.protocolSupports = protocolSupports;
        this.elasticSearchService = elasticSearchService;
    }

    @Override
    public String getId() {
        return "mqtt-server-gateway";
    }

    @Override
    public String getName() {
        return "MQTT直连接入";
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.MQTT_SERVER;
    }

    @Resource
    private IotDevTypeProducer iotDevTypeProducer;

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {

        return networkManager
                .<MqttServer>getNetwork(getNetworkType(), properties.getNetworkId())
                .map(mqttServer -> new MqttServerDeviceGateway(
                        properties.getId(),
                        registry,
                        sessionManager,
                        mqttServer,
                        messageHandler,
                        properties.getString("protocol")
                                .map(id -> Mono.defer(() -> protocolSupports.getProtocol(id)))
                                .orElse(Mono.empty()),
                        elasticSearchService,
                        iotDevTypeProducer));
    }
}
