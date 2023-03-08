package top.iot.gateway.network.http.server;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SocketAddress;
import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpServerProperties /*implements ValueObject*/ {

    //cluster:true(共享配置)、false(独立配置)
    //{"id":"1280380104309391360","name":"测试HTTP2-9114","type":"HTTP_SERVER","shareCluster":true,"configuration":{"port":"9114","certId":"1214366803375415296","ssl":false},"describe":"11"}
    //{"id":"1280380104309391360","name":"测试HTTP2-9114","type":"HTTP_SERVER","shareCluster":false,"cluster":[{"configuration":{"port":"","certId":"","ssl":false},"serverId":"127.0.0.1:8844"}]}
    private String id;

    private String name;

    private String type;

    private boolean shareCluster;

    //cluster:true(共享配置)
    //{"port":"9114","certId":"1214366803375415296","ssl":false}
    private Map<String, Object> configurations = new HashMap<>();

    //cluster:false(独立配置)
    //[{"configuration":{"port":"","certId":"","ssl":false},"serverId":"127.0.0.1:8844"}]
    private List<Map<String, Object>> cluster = new ArrayList<>();

    private String describe;

    private HttpServerOptions options;

    public SocketAddress createSocketAddress() {
        return SocketAddress.inetSocketAddress(
                Integer.parseInt(String.valueOf(configurations.getOrDefault("port", 80))),
                String.valueOf(configurations.getOrDefault("host", "0.0.0.0")));
    }

}