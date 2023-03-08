package top.iot.gateway.network.udp.device;

import com.alibaba.fastjson.JSON;
import top.iot.gateway.component.elasticsearch.service.ElasticSearchService;
import top.iot.gateway.component.gateway.DeviceGateway;
import top.iot.gateway.component.gateway.supports.DeviceGatewayProperties;
import top.iot.gateway.component.gateway.supports.DeviceGatewayProvider;
import top.iot.gateway.network.DefaultNetworkType;
import top.iot.gateway.network.NetworkManager;
import top.iot.gateway.network.NetworkType;
import top.iot.gateway.network.SessionIdDeviceIdBindingRegistry;
import top.iot.gateway.network.mq.devtype.IotDevTypeProducer;
import top.iot.gateway.network.udp.client.UdpClient;
import top.iot.gateway.core.ProtocolSupports;
import top.iot.gateway.core.device.DeviceRegistry;
import top.iot.gateway.core.device.session.DeviceSessionManager;
import top.iot.gateway.supports.server.DecodedClientMessageHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

@Component
public class UdpDeviceGatewayProvider implements DeviceGatewayProvider {

    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler messageHandler;

    private final ProtocolSupports protocolSupports;

    private final SessionIdDeviceIdBindingRegistry sessionIdDeviceIdBindingRegistry;

    private final ElasticSearchService elasticSearchService;

    public UdpDeviceGatewayProvider(NetworkManager networkManager,
                                    DeviceRegistry registry,
                                    DeviceSessionManager sessionManager,
                                    DecodedClientMessageHandler messageHandler,
                                    ProtocolSupports protocolSupports,
                                    ElasticSearchService elasticSearchService,
                                    SessionIdDeviceIdBindingRegistry sessionIdDeviceIdBindingRegistry) {
        this.networkManager = networkManager;
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.messageHandler = messageHandler;
        this.protocolSupports = protocolSupports;
        this.sessionIdDeviceIdBindingRegistry = sessionIdDeviceIdBindingRegistry;
        this.elasticSearchService = elasticSearchService;
    }

    @Override
    public String getId() {
        return "udp-gateway";
    }

    @Override
    public String getName() {
        return "UDP接入";
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.UDP;
    }

    @Resource
    private IotDevTypeProducer iotDevTypeProducer;

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return networkManager
                .<UdpClient>getNetwork(getNetworkType(), properties.getNetworkId())
                .map(udpClient -> {
                    String protocol = (String) properties.getConfiguration().get("protocol");

                    Assert.hasText(protocol,"protocol can not be empty");

                    String deviceCode = properties.getConfiguration().get("deviceCode")==null ? null : JSON.toJSONString(properties.getConfiguration().get("deviceCode"));


                    return new UdpDeviceGateway(properties.getId(),
                            protocol,
                            protocolSupports,
                            registry,
                            messageHandler,
                            sessionManager,
                            udpClient,
                            elasticSearchService,
                            deviceCode,
                            sessionIdDeviceIdBindingRegistry,
                            iotDevTypeProducer
                    );
                });
    }
}
