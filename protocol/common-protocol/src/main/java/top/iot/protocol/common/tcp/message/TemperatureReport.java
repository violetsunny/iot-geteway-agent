package top.iot.protocol.common.tcp.message;

import top.iot.protocol.common.tcp.TcpDeviceMessage;
import top.iot.protocol.common.tcp.TcpPayload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.message.property.ReportPropertyMessage;
import top.iot.gateway.core.utils.BytesUtils;

import java.util.Collections;

@Getter
@Setter
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
public class TemperatureReport implements TcpPayload, TcpDeviceMessage {

    private long deviceId;

    private float temperature;

    @Override
    public DeviceMessage toDeviceMessage() {
        ReportPropertyMessage message = new ReportPropertyMessage();
        message.setProperties(Collections.singletonMap("temperature", temperature));
        message.setDeviceId(String.valueOf(deviceId));
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }

    @Override
    public byte[] toBytes() {
        //前8位为设备ID,后4位为温度值,低位字节在前.
        byte[] data = new byte[12];
        BytesUtils.numberToLe(data, deviceId, 0, 8);
        BytesUtils.numberToLe(data, Float.floatToIntBits(temperature), 8, 4);
        return data;
    }


    public static void main(String[] args) {
        int sid = 123;
        float t = 23.1f;
        byte[] data = new byte[12];
        BytesUtils.numberToLe(data, sid, 0 , 8);
        BytesUtils.numberToLe(data, Float.floatToIntBits(t), 8, 4);
        System.out.println(data);

    }

    @Override
    public void fromBytes(byte[] bytes, int offset) {
        this.deviceId = BytesUtils.leToLong(bytes, offset, 8);
        this.temperature = BytesUtils.leToFloat(bytes, offset + 8, 4);
    }

    @Override
    public String toString() {
        return "TemperatureReport{" +
                "deviceId=" + deviceId +
                ", temperature=" + temperature +
                '}';
    }
}
