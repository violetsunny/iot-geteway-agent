package top.iot.gateway.manager.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class MqttMessageResponse {

    private int messageId;

    private Object payload;

    private String topic;

    private int qosLevel;

    private boolean dup;
}
