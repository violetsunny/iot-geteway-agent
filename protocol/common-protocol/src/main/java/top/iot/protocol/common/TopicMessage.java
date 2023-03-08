package top.iot.protocol.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import top.iot.gateway.core.message.DeviceMessage;

@Getter
@Setter
@AllArgsConstructor
public class TopicMessage {

    private String topic;

    private Object message;
}
