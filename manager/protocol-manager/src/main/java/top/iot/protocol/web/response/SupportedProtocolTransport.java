package top.iot.protocol.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.web.api.crud.entity.GenericEntity;

import javax.persistence.Column;
import javax.persistence.Table;

@Data
@Table(name = "supported_protocol_transport")
@NoArgsConstructor
public class SupportedProtocolTransport extends GenericEntity<Integer> {

    @Comment("id")
    @Column
    private  Integer id;

    @Comment("消息协议")
    @Column
    @Schema(description = "消息协议")
    private  String protocol;

    @Comment("传输协议")
    @Column
    @Schema(description = "传输协议")
    private  String transport;

}
