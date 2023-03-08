package top.iot.protocol.web.response;

import top.iot.protocol.entity.ProtocolSupportEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.iot.gateway.core.ProtocolSupport;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class ProtocolInfo {
    private String id;

    private String name;

    public static ProtocolInfo of(ProtocolSupport support) {
        return of(support.getId(), support.getName());
    }

    public static ProtocolInfo of(ProtocolSupportEntity support) {
        return of(support.getId(), support.getName());
    }
}