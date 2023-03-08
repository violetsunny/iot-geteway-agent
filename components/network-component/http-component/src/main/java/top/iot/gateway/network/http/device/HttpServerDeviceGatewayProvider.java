package top.iot.gateway.network.http.device;

import com.alibaba.fastjson.JSONArray;
import top.iot.gateway.component.elasticsearch.service.ElasticSearchService;
import top.iot.gateway.component.gateway.DeviceGateway;
import top.iot.gateway.component.gateway.supports.DeviceGatewayProperties;
import top.iot.gateway.component.gateway.supports.DeviceGatewayProvider;
import top.iot.gateway.network.DefaultNetworkType;
import top.iot.gateway.network.NetworkManager;
import top.iot.gateway.network.NetworkType;
import top.iot.gateway.network.http.server.HttpServer;
import top.iot.gateway.network.mq.devtype.IotDevTypeProducer;
import lombok.Data;
import top.iot.gateway.core.ProtocolSupports;
import top.iot.gateway.core.device.DeviceRegistry;
import top.iot.gateway.core.device.session.DeviceSessionManager;
import top.iot.gateway.supports.server.DecodedClientMessageHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.List;


@Component
public class HttpServerDeviceGatewayProvider implements DeviceGatewayProvider {

    private final NetworkManager networkManager;

    private final DeviceRegistry registry;

    private final DeviceSessionManager sessionManager;

    private final DecodedClientMessageHandler messageHandler;

    private final ProtocolSupports protocolSupports;

    private final ElasticSearchService elasticSearchService;

    public HttpServerDeviceGatewayProvider(NetworkManager networkManager,
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
        return "http-server-gateway";
    }

    @Override
    public String getName() {
        return "HTTP服务设备网关";
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.HTTP_SERVER;
    }

    @Resource
    private IotDevTypeProducer iotDevTypeProducer;

    @Override
    public Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties) {
        return networkManager
                .<HttpServer>getNetwork(getNetworkType(), properties.getNetworkId())
                .map(httpServer -> {
//                String protocol = "GodsonIot v1.0";
//                String protocol = (String) properties.getConfiguration().get("routes").get("protocol");

                    List<GateWayRouter> routes = JSONArray.parseArray(JSONArray.toJSONString(properties.getConfiguration().get("routes")), GateWayRouter.class);

                    return new HttpServerDeviceGateway(properties.getId(),
                            routes,
                            protocolSupports,
                            registry,
                            messageHandler,
                            sessionManager,
                            httpServer,
                            elasticSearchService,
                            iotDevTypeProducer
                    );
                });
    }
}

@Data
class GateWayRouter {
    private String id;
    private String url;
    private String protocol;
}