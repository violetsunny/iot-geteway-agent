package top.iot.gateway.manager.entity;

import top.iot.gateway.manager.enums.DeviceState;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class DeviceStateInfo {
    private String deviceId;

    private DeviceState state;
}
