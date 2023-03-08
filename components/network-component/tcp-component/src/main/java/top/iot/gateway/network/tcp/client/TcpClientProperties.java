package top.iot.gateway.network.tcp.client;

import top.iot.gateway.network.tcp.parser.PayloadParserType;
import io.vertx.core.net.NetClientOptions;
import lombok.*;
import top.iot.gateway.component.common.ValueObject;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TcpClientProperties implements ValueObject {

    private String id;

    private int port;

    private String host;

    private String certId;

    private boolean ssl;

    private PayloadParserType parserType;

    private Map<String, Object> parserConfiguration = new HashMap<>();

    private NetClientOptions options;

    private boolean enabled;

    @Override
    public Map<String, Object> values() {
        return parserConfiguration;
    }
}
