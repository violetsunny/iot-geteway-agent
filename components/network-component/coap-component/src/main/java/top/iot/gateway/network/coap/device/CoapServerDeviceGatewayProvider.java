package top.iot.gateway.network.coap.device;

import top.iot.gateway.component.gateway.DeviceGateway;
import top.iot.gateway.component.gateway.supports.DeviceGatewayProperties;
import top.iot.gateway.component.gateway.supports.DeviceGatewayProvider;
import top.iot.gateway.network.DefaultNetworkType;
import top.iot.gateway.network.NetworkManager;
import top.iot.gateway.network.NetworkType;
import top.iot.gateway.network.coap.server.COAPServer;
import top.iot.gateway.network.mq.devtype.IotDevTypeProducer;
import top.iot.gateway.core.ProtocolSupports;
import top.iot.gateway.core.device.DeviceRegistry;
import top.iot.gateway.core.device.session.DeviceSessionManager;
import top.iot.gateway.supports.server.DecodedClientMessageHandler;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

//@Component
public class CoapServerDeviceGatewayProvider implements DeviceGatewayProvider {
    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler messageHandler;

    private final ProtocolSupports protocolSupports;

    public CoapServerDeviceGatewayProvider(NetworkManager networkManager,
                                           DeviceRegistry registry,
                                           DeviceSessionManager sessionManager,
                                           DecodedClientMessageHandler messageHandler,
                                           ProtocolSupports protocolSupports) {
        this.networkManager = networkManager;
        this.registry = registry;
        this.sessionManager = sessionManager;
        this.messageHandler = messageHandler;
        this.protocolSupports = protocolSupports;
    }

    @Override
    public String getId() {
        return "coap-server-gateway";
    }

    @Override
    public String getName() {
        return "COAP服务接入";
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.COAP_SERVER;
    }

    @Resource
    private IotDevTypeProducer iotDevTypeProducer;

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return networkManager
            .<COAPServer>getNetwork(getNetworkType(), properties.getNetworkId())
            .map(coapServer -> new CoapServerDeviceGateway(
                    properties.getId(),
                    registry,
                    sessionManager,
                    coapServer,
                    messageHandler,
                    properties.getString("protocol")
                        .map(id -> Mono.defer(() -> protocolSupports.getProtocol(id)))
                        .orElse(Mono.empty()),
                    iotDevTypeProducer
                ));
    }
}
