package top.iot.protocol.entity;

import com.alibaba.fastjson.JSON;
import top.iot.protocol.godson.metadataMapping.ProtocolConfigInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.Pattern;
import java.sql.JDBCType;

@Getter
@Setter
@Table(name = "dev_protocol_config", indexes = {
    @Index(name = "idx_protocol_config_class_id", columnList = "type")
})
@NoArgsConstructor
public class DeviceProtocolConfigEntity extends GenericEntity<String> {

    @Override
    @GeneratedValue(generator = Generators.SNOW_FLAKE)
    @Pattern(
        regexp = "^[0-9a-zA-Z_\\-]+$",
        message = "ID只能由数字,字母,下划线和中划线组成",
        groups = CreateGroup.class)
    @Schema(description = "ID")
    public String getId() {
        return super.getId();
    }

    @Comment("消息类型(JSON/STRING/BINARY)")
    @Column
    @Schema(description = "消息类型(JSON/STRING/BINARY)")
    private String type;

    @Comment("物模型映射")
    @Column(name = "metadata_mapping")
    @ColumnType(jdbcType = JDBCType.CLOB)
    @Schema(description = "物模型映射")
    private String metadataMapping;

    @Comment("二进制消息配置信息")
    @Column(name = "msg_info")
    @ColumnType(jdbcType = JDBCType.CLOB)
    @Schema(description = "二进制消息配置信息")
    private String msgInfo;

    @Comment("二进制消息类型(认证、认证回复、心跳、上报、上报回复)")
    @Column(name = "msg_type")
    @ColumnType(jdbcType = JDBCType.CLOB)
    @Schema(description = "二进制消息类型(认证、认证回复、心跳、上报、上报回复)")
    private String msgType;

    public ProtocolConfigInfo toProtocolConfigInfo() {
        return ProtocolConfigInfo
            .builder()
            .id(getId())
            .metadataMapping(JSON.toJSONString(this))
            .build();
    }

}
