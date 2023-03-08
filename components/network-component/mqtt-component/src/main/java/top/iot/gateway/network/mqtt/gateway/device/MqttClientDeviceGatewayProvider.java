package top.iot.gateway.network.mqtt.gateway.device;

import com.alibaba.fastjson.JSON;
import top.iot.gateway.component.gateway.DeviceGateway;
import top.iot.gateway.component.gateway.supports.DeviceGatewayProperties;
import top.iot.gateway.component.gateway.supports.DeviceGatewayProvider;
import top.iot.gateway.component.elasticsearch.service.ElasticSearchService;
import top.iot.gateway.network.DefaultNetworkType;
import top.iot.gateway.network.NetworkManager;
import top.iot.gateway.network.NetworkType;
import top.iot.gateway.network.mq.devtype.IotDevTypeProducer;
import top.iot.gateway.network.mqtt.client.MqttClient;
import top.iot.gateway.core.ProtocolSupports;
import top.iot.gateway.core.device.DeviceRegistry;
import top.iot.gateway.core.device.session.DeviceSessionManager;
import top.iot.gateway.supports.server.DecodedClientMessageHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Objects;

@Component
public class MqttClientDeviceGatewayProvider implements DeviceGatewayProvider {
    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler clientMessageHandler;

    private final ProtocolSupports protocolSupports;

    private final ElasticSearchService elasticSearchService;

    public MqttClientDeviceGatewayProvider(NetworkManager networkManager,
                                           DeviceRegistry registry,
                                           DeviceSessionManager sessionManager,
                                           DecodedClientMessageHandler clientMessageHandler,
                                           ProtocolSupports protocolSupports,
                                           ElasticSearchService elasticSearchService) {
        this.networkManager = networkManager;
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.clientMessageHandler = clientMessageHandler;
        this.protocolSupports = protocolSupports;
        this.elasticSearchService = elasticSearchService;
    }

    @Resource
    private IotDevTypeProducer iotDevTypeProducer;

    @Override
    public String getId() {
        return "mqtt-client-gateway";
    }

    @Override
    public String getName() {
        return "MQTT Broker接入";
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.MQTT_CLIENT;
    }

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return networkManager
                .<MqttClient>getNetwork(getNetworkType(), properties.getNetworkId())
                .map(mqttClient -> {

                    String protocol = (String) properties.getConfiguration().get("protocol");
                    String topics = (String) properties.getConfiguration().get("topics");
                    int qos = properties.getInt("qos").orElse(0);
                    Objects.requireNonNull(topics, "topics");
                    String deviceCode = properties.getConfiguration().get("deviceCode")==null ? null : JSON.toJSONString(properties.getConfiguration().get("deviceCode"));

                    return new MqttClientDeviceGateway(properties.getId(),
                            mqttClient,
                            registry,
                            protocolSupports,
                            protocol,
                            sessionManager,
                            clientMessageHandler,
                            Arrays.asList(topics.split("[,;\n]")),
                            qos,
                            elasticSearchService,
                            deviceCode,
                            iotDevTypeProducer
                    );

                });
    }
}
