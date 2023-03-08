package top.iot.gateway.manager.api.request;


import lombok.*;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Builder
@Getter
@Setter
public class MqttMessageRequest {

    private String topic;

    private int qosLevel;

    private Object data;

    private int messageId;

    private boolean will;

    private boolean dup;

    private boolean retain;

}
