package top.iot.gateway.network;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@Data
public class DeviceOriginalMessage implements Serializable {
    private String deviceId;
    private String payload;
}
