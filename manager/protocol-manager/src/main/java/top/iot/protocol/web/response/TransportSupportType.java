package top.iot.protocol.web.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TransportSupportType{
    ENCODE("编码"), DECODE("解码");
    private String text;

    public String getValue() {
        return name();
    }

}
