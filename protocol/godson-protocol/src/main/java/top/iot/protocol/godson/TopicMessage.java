package top.iot.protocol.godson;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TopicMessage {

    private String topic;

    private Object message;
}
