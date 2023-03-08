package top.iot.gateway.network.udp.client;

import io.vertx.core.datagram.DatagramSocketOptions;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UdpClientProperties {

    private String id;

    private String remoteAddress;

    private Integer remotePort;

    private String localAddress;

    private Integer localPort;

    private Integer instance = Runtime.getRuntime().availableProcessors();

    private DatagramSocketOptions options;

    private boolean ssl;
}
