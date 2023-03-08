package top.iot.protocol.godson.metadataMapping;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolConfigInfo implements Serializable {

    private String id;

    private String metadataMapping;

}
