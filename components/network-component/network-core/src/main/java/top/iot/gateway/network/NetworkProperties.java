package top.iot.gateway.network;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
public class NetworkProperties implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;

    private String name;

    private String serverId;

    private boolean enabled;

    private Map<String, Object> configurations;

}
