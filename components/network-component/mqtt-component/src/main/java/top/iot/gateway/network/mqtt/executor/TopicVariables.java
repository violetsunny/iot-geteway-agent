package top.iot.gateway.network.mqtt.executor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.iot.gateway.rule.engine.api.RuleDataCodec;

import java.util.List;

@Getter
@AllArgsConstructor
public class TopicVariables implements RuleDataCodec.Feature {
    List<String> variables;
}
