package top.iot.gateway.agent.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "top.iot")
@Getter
@Setter
public class IotGatewayProperties {

    @Autowired
    private Environment environment;

    private String serverId;

    private String clusterName ="default";

    private Map<String, Long> transportLimit;

    private Map<String, Object> tags = new HashMap<>();

    @PostConstruct
    @SneakyThrows
    public void init() {
        String address = getIpAddress() + ":" + this.environment.getProperty("server.port");
        if (this.serverId == null) {
//            serverId = InetAddress.getLocalHost().getHostName();
//            System.out.println(InetAddress.getLocalHost().getHostAddress());
            this.serverId = address;
        }
        this.tags.put("address", address);
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
}
