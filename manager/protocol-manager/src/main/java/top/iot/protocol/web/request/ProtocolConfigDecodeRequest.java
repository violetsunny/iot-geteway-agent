package top.iot.protocol.web.request;

import lombok.Getter;
import lombok.Setter;
import top.iot.gateway.core.message.codec.MessagePayloadType;
import top.iot.gateway.core.metadata.DeviceMetadataType;

@Getter
@Setter
public class ProtocolConfigDecodeRequest {
    private DeviceMetadataType deviceMetadataType;
    private MessagePayloadType payloadType;
    private String payload;
    private String metadataMappings;
}
