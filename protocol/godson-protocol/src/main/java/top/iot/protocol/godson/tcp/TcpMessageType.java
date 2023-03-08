package top.iot.protocol.godson.tcp;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TcpMessageType {
    AUTH_REQ("认证请求"),
    AUTH_RES("认证结果"),
    ERROR("错误"),
    PING("Ping"), PONG("Pong"),
    REPORT_TEMPERATURE("上报温度"),
    //    READ_TEMPERATURE("读取温度"),
//    READ_TEMPERATURE_REPLY("读取温度回复"),
    FIRE_ALARM("火警"),
    READ_PROPERTY("读取设备属性"),
    WRITE_PROPERTY("修改设备属性"),
    REPORT_PROPERTY("上报设备属性"),
    INVOKE_FUNCTION("函数调用");

    private String text;

    public byte[] toBytes(TcpPayload data) {
        if (data == null) {
            return new byte[0];
        }
        return data.toBytes();
    }
}
