package top.iot.protocol.web.request;

import top.iot.protocol.entity.ProtocolSupportEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProtocolEncodeRequest {

    ProtocolSupportEntity entity;

    ProtocolEncodePayload request;


}
