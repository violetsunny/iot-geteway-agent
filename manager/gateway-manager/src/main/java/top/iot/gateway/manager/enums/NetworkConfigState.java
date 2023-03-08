package top.iot.gateway.manager.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.EnumDict;

import java.util.stream.Stream;

@AllArgsConstructor
@Getter
@Dict("network-config-state")
public enum NetworkConfigState implements EnumDict<String> {
    enabled("已启动", "enabled"),
    paused("已暂停", "paused"),
    disabled("已停止", "disabled");

    private String text;

    private String value;

    public static NetworkConfigState toEnum(String state) {
        if (StringUtils.isEmpty(state)) {
            return null;
        }
        return Stream.of(values()).filter(v -> v.getValue().equals(state)).findFirst().orElse(null);
    }
}
