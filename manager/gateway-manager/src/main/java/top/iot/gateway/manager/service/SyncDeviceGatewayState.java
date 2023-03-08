package top.iot.gateway.manager.service;

import top.iot.gateway.component.gateway.DeviceGateway;
import top.iot.gateway.component.gateway.DeviceGatewayManager;
import top.iot.gateway.manager.entity.DeviceGatewayEntity;
import top.iot.gateway.manager.enums.NetworkConfigState;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Duration;
import java.util.Enumeration;

/**
 * @author wangzheng
 * @since 1.0
 */

@Order(1)
@Component
@Slf4j
@Getter
@Setter
@ConfigurationProperties(prefix = "nyiot")
public class SyncDeviceGatewayState implements CommandLineRunner {

    private String serverId;

    private final DeviceGatewayService deviceGatewayService;

    private final DeviceGatewayManager deviceGatewayManager;

    private final Duration gatewayStartupDelay = Duration.ofSeconds(5);

    private final Environment environment;

    public SyncDeviceGatewayState(DeviceGatewayService deviceGatewayService, DeviceGatewayManager deviceGatewayManager, Environment environment) {
        this.deviceGatewayService = deviceGatewayService;
        this.deviceGatewayManager = deviceGatewayManager;
        this.environment = environment;
    }

    @PostConstruct
    @SneakyThrows
    public void init() {
        String address = getIpAddress() + ":" + this.environment.getProperty("server.port");
        if (this.serverId == null) {
            this.serverId = address;
        }
    }

    private String getIpAddress() throws SocketException {
        String ip="";
        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
            NetworkInterface intf = en.nextElement();
            String name = intf.getName();
            if (!name.contains("docker") && !name.contains("lo")) {
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    //获得IP
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        String ipaddress = inetAddress.getHostAddress().toString();
                        if (!ipaddress.contains("::") && !ipaddress.contains("0:0:") && !ipaddress.contains("fe80")) {
                            if(!"127.0.0.1".equals(ip)){
                                ip = ipaddress;
                            }
                        }
                    }
                }
            }
        }
        return ip;
    }

    @Override
    public void run(String... args) {
        log.info("{} start device gateway in {} later",this.serverId, this.gatewayStartupDelay);
        Mono.delay(this.gatewayStartupDelay)
            .then(
                deviceGatewayService
                    .createQuery()
                    .where()
                    .and(DeviceGatewayEntity::getState, NetworkConfigState.enabled)
                    .and("serverId",this.serverId)
                    .fetch()
                    .map(DeviceGatewayEntity::getId)
                    .flatMap(deviceGatewayManager::getGateway)
                    .flatMap(DeviceGateway::startup)
                    .onErrorContinue((err, obj) -> {
                        log.error(err.getMessage(), err);
                    })
                    .then()
            )
            .subscribe();
    }
}
