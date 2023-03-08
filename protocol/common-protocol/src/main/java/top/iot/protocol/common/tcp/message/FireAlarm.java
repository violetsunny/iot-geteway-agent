package top.iot.protocol.common.tcp.message;

import top.iot.protocol.common.tcp.TcpDeviceMessage;
import top.iot.protocol.common.tcp.TcpPayload;
import lombok.*;
import org.hswebframework.web.id.IDGenerator;
import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.message.event.EventMessage;
import top.iot.gateway.core.utils.BytesUtils;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class FireAlarm implements TcpPayload, TcpDeviceMessage {

    //设备ID
    private long deviceId;

    //经度
    private float lnt;

    //纬度
    private float lat;

    //点位
    private int point;

    private String bName;

    @Override
    public DeviceMessage toDeviceMessage() {
        EventMessage message = new EventMessage();
        message.setEvent("fire_alarm");
        message.setMessageId(IDGenerator.SNOW_FLAKE_STRING.generate());
        Map<String, Object> map = new HashMap<>();
        map.put("lnt", lnt);
        map.put("lat", lat);
        map.put("point", point);
        map.put("b_name", bName);
        message.setData(map);
        message.setDeviceId(String.valueOf(deviceId));
        return message;
    }

    @Override
    public byte[] toBytes() {
        //设备id+经度+维度+点位
        byte[] data = new byte[8 + 4 + 4 + 4];
        BytesUtils.numberToLe(data, deviceId, 0, 8);
        BytesUtils.numberToLe(data, Float.floatToIntBits(lnt), 8, 4);
        BytesUtils.numberToLe(data, Float.floatToIntBits(lat), 12, 4);
        BytesUtils.numberToLe(data, point, 16, 4);
        return data;
    }

    @Override
    public void fromBytes(byte[] bytes, int offset) {
        setDeviceId(BytesUtils.leToLong(bytes, offset, 8));
        setLnt(BytesUtils.leToFloat(bytes, offset + 8, 4));
        setLat(BytesUtils.leToFloat(bytes, offset + 12, 4));
        setPoint(BytesUtils.leToInt(bytes, offset + 16, 4));
    }
}
