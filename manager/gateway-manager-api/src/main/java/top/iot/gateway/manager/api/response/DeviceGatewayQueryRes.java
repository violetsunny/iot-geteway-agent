package top.iot.gateway.manager.api.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class DeviceGatewayQueryRes {
    private String id;
    private String name;
    private String serverId;
    private String provider;
    private String providerName;
    private String networkId;
    private String networkName;
    private Map<String,String> state;
    private Map<String,Object> configuration;
    private String describe;
    private Date createTime;
    private Date updateTime;

}
