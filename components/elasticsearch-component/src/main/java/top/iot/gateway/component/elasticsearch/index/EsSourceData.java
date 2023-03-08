package top.iot.gateway.component.elasticsearch.index;

import lombok.*;

import java.io.Serializable;

/**
 * @author shq
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class EsSourceData extends EsBaseData {

    private static final long serialVersionUID = 5516075349620653480L;

    private long timestamp;

    private String deviceId;

    private String type;

    private Object source;

    public EsSourceData(String deviceId, String type, Object source) {
        this.deviceId = deviceId;
        this.type = type;
        this.source = source;
        this.timestamp = System.currentTimeMillis();;
    }
}
