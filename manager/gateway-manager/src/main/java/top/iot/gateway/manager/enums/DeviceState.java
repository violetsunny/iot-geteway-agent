package top.iot.gateway.manager.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.I18nEnumDict;

@AllArgsConstructor
@Getter
@Dict("device-state")
public enum DeviceState implements I18nEnumDict<String> {
    notActive("未激活"),
    offline("离线"),
    online("在线");

    private final String text;

    @Override
    public String getValue() {
        return name();
    }

    public static DeviceState of(byte state) {
        switch (state) {
            case top.iot.gateway.core.device.DeviceState.offline:
                return offline;
            case top.iot.gateway.core.device.DeviceState.online:
                return online;
            default:
                return notActive;
        }
    }

    public static Integer of(DeviceState state) {
        switch (state) {
            case offline:
                return (int) top.iot.gateway.core.device.DeviceState.offline;
            case online:
                return (int) top.iot.gateway.core.device.DeviceState.online;
            default:
                return (int) top.iot.gateway.core.device.DeviceState.noActive;
        }
    }
}
