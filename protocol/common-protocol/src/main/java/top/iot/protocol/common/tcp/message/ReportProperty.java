package top.iot.protocol.common.tcp.message;

import com.alibaba.fastjson.JSON;
import top.iot.protocol.common.tcp.TcpPayload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.iot.gateway.core.message.property.ReportPropertyMessage;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class ReportProperty implements TcpPayload {
    private ReportPropertyMessage reportPropertyMessage;

    @Override
    public byte[] toBytes() {
        return reportPropertyMessage.toString().getBytes();
    }

    @Override
    public void fromBytes(byte[] bytes, int offset) {
        ReportPropertyMessage reportPropertyMessage = new ReportPropertyMessage();
        byte[] bytes1 = Arrays.copyOfRange(bytes, offset, bytes.length);
        String s = new String(bytes1, StandardCharsets.UTF_8);
        reportPropertyMessage.fromJson(JSON.parseObject(s));
        this.reportPropertyMessage = reportPropertyMessage;
    }
}
