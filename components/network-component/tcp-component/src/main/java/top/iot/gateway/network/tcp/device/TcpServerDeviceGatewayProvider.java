package top.iot.gateway.network.tcp.device;

import com.alibaba.fastjson.JSON;
import top.iot.gateway.component.elasticsearch.service.ElasticSearchService;
import top.iot.gateway.network.NetworkManager;
import top.iot.gateway.network.DefaultNetworkType;
import top.iot.gateway.network.NetworkType;
import top.iot.gateway.component.gateway.DeviceGateway;
import top.iot.gateway.component.gateway.supports.DeviceGatewayProperties;
import top.iot.gateway.component.gateway.supports.DeviceGatewayProvider;
import top.iot.gateway.network.SessionIdDeviceIdBindingRegistry;
import top.iot.gateway.network.mq.devtype.IotDevTypeProducer;
import top.iot.gateway.network.tcp.server.TcpServer;
import top.iot.gateway.core.ProtocolSupports;
import top.iot.gateway.core.device.DeviceRegistry;
import top.iot.gateway.core.device.session.DeviceSessionManager;
import top.iot.gateway.supports.server.DecodedClientMessageHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

/**
 * TCP服务设备网关提供商
 *
 * @author hanyl
 * @since 1.0
 */
@Component
public class TcpServerDeviceGatewayProvider implements DeviceGatewayProvider {

    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler messageHandler;

    private final ProtocolSupports protocolSupports;

    private final ElasticSearchService elasticSearchService;

    private final SessionIdDeviceIdBindingRegistry sessionIdDeviceIdBindingRegistry;


    public TcpServerDeviceGatewayProvider(NetworkManager networkManager,
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
        this.elasticSearchService = elasticSearchService;
        this.sessionIdDeviceIdBindingRegistry = sessionIdDeviceIdBindingRegistry;
    }

    @Override
    public String getId() {
        return "tcp-server-gateway";
    }

    @Override
    public String getName() {
        return "TCP 透传接入";
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.TCP_SERVER;
    }

    @Resource
    private IotDevTypeProducer iotDevTypeProducer;

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return networkManager
                .<TcpServer>getNetwork(getNetworkType(), properties.getNetworkId())
                .map(server -> {
                    String protocol = (String) properties.getConfiguration().get("protocol");

                    //Assert.hasText(protocol, "protocol can not be empty");

                    String deviceCode = properties.getConfiguration().get("deviceCode")==null ? null : JSON.toJSONString(properties.getConfiguration().get("deviceCode"));

                    return new TcpServerDeviceGateway(properties.getId(),
                            protocol,
                            protocolSupports,
                            registry,
                            messageHandler,
                            sessionManager,
                            server,
                            elasticSearchService,
                            deviceCode,
                            sessionIdDeviceIdBindingRegistry,
                            iotDevTypeProducer
                    );
                });
    }
}
