package top.iot.gateway.network.http.client;

import top.iot.gateway.component.common.ValueObject;
import io.vertx.core.http.HttpClientOptions;
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
public class HttpClientProperties  implements ValueObject {

    //{"name":"zz","type":"HTTP_CLIENT","shareCluster":true,"describe":"xx","configuration":{"baseUrl":"xx","certId":"1214366701554491392","ssl":false,"verifyHost":false,"trustAll":false}}

    private String id;

    private String name;

    private String type;

    private boolean shareCluster;

    private Map<String, Object> configurations = new HashMap<>();

    private List<Map<String, Object>> cluster = new ArrayList<>();

    private String describe;

    private HttpClientOptions options;

    @Override
    public Map<String, Object> values() {
        return configurations;
    }
}