package top.iot.protocol.web.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import top.iot.gateway.core.ProtocolSupport;
import top.iot.gateway.core.message.codec.Transport;
import reactor.core.publisher.Mono;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
public class TransportDetail {
    private String id;

    private String name;

    public static Mono<TransportDetail> of(ProtocolSupport support, Transport transport) {
        return Mono.just(new TransportDetail(transport.getId(), transport.getName()));
    }


}