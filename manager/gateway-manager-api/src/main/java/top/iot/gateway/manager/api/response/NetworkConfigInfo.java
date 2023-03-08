package top.iot.gateway.manager.api.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class NetworkConfigInfo {
    private String id;

    private String name;

    private String address;

    public String getDetail() {
        return name;
    }
}