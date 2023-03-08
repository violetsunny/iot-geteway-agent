package top.iot.gateway.network.mqtt.executor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.iot.gateway.rule.engine.api.RuleDataCodec;

import java.util.List;

@Getter
@AllArgsConstructor
public class MqttTopics implements RuleDataCodec.Feature {

    private List<String> topics;

    @Override
    public String getId() {
        return "mqtt-topic";
    }

    @Override
    public String getName() {
        return "MQTT Topics";
    }
}
