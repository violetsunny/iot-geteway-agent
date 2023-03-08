package top.iot.gateway.manager.api.protocol;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
public class TransportDetail {
    private String id;

    private String name;


}