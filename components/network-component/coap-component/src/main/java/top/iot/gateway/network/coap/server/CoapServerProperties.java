package top.iot.gateway.network.coap.server;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoapServerProperties {
    private String id;

    private String address;

    private Integer port;

    private Long timeout;

    private boolean enableDtls;
}
