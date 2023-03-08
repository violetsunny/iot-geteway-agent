package top.iot.gateway.network;

import top.iot.gateway.component.elasticsearch.index.ElasticIndex;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DeviceMessageIndexProvider implements ElasticIndex {

    DEVICE_ORIGINAL_MESSAGE("device_origianl_message");

    private String index;
}
