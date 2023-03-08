package top.iot.gateway.network.coap.server;

import top.iot.gateway.network.DefaultNetworkType;
import top.iot.gateway.network.NetworkType;
import top.iot.gateway.network.coap.device.CoapTransportResource;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.elements.config.Configuration;
import reactor.core.publisher.Flux;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DefaultCoapServer implements COAPServer {

    @Getter
    private String id;

    private CoapServer coapServer;

    public DefaultCoapServer(CoapServerProperties properties) {
        this.coapServer = createCoapServer(properties);
    }

    @SneakyThrows
    public CoapServer createCoapServer(CoapServerProperties properties) {
        this.id = properties.getId();
        Configuration networkConfig = Configuration.createStandardWithoutFile();
        networkConfig.set(CoapConfig.BLOCKWISE_STRICT_BLOCK2_OPTION, true);
        networkConfig.set(CoapConfig.BLOCKWISE_ENTITY_TOO_LARGE_AUTO_FAILOVER, true);
        networkConfig.set(CoapConfig.BLOCKWISE_STATUS_LIFETIME, 5 * 60, TimeUnit.SECONDS);
        networkConfig.set(CoapConfig.MAX_RESOURCE_BODY_SIZE, 256 * 1024 * 1024);
        networkConfig.set(CoapConfig.RESPONSE_MATCHING, CoapConfig.MatcherMode.RELAXED);
        networkConfig.set(CoapConfig.PREFERRED_BLOCK_SIZE, 1024);
        networkConfig.set(CoapConfig.MAX_MESSAGE_SIZE, 1024);
        networkConfig.set(CoapConfig.MAX_RETRANSMIT, 4);
        networkConfig.set(CoapConfig.COAP_PORT, properties.getPort());
        coapServer = new CoapServer(networkConfig,properties.getPort());

        CoapEndpoint.Builder noSecCoapEndpointBuilder = new CoapEndpoint.Builder();
        InetAddress addr = InetAddress.getByName(properties.getAddress());
        InetSocketAddress sockAddr = new InetSocketAddress(addr, properties.getPort());
        noSecCoapEndpointBuilder.setInetSocketAddress(sockAddr);

        noSecCoapEndpointBuilder.setConfiguration(networkConfig);
        CoapEndpoint noSecCoapEndpoint = noSecCoapEndpointBuilder.build();
        coapServer.addEndpoint(noSecCoapEndpoint);
        //todo 支持DTLS协议
        Resource root = coapServer.getRoot();
        CoapServerMessageDeliverer messageDeliverer = new CoapServerMessageDeliverer(root);
        coapServer.setMessageDeliverer(messageDeliverer);

        CoapResource api = new CoapResource("api");
        api.add(new CoapTransportResource("v1"));
        coapServer.add(api);
        coapServer.start();
        return coapServer;
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.COAP_SERVER;
    }

    @Override
    public void shutdown() {
        if (null != coapServer) {
            execute(coapServer::destroy);
            coapServer = null;
        }
    }

    @Override
    public boolean isAlive() {
        return coapServer!=null;
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("close coap server error", e);
        }
    }

    @Override
    public Flux<CoapConnection> handleConnection() {
        return Flux.just(coapServer.getMessageDeliverer())
            .cast(CoapServerMessageDeliverer.class)
            .flatMap(CoapServerMessageDeliverer::handleConnection);
    }
}
