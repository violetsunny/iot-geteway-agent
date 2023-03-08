package top.iot.protocol.entity;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.generator.Generators;
import top.iot.gateway.supports.protocol.management.ProtocolSupportDefinition;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.Date;
import java.util.Map;

@Getter
@Setter
@Table(name = "dev_protocol")
public class ProtocolSupportEntity extends GenericEntity<String> {

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String type;

    @Column
    private Byte state;

    @Column
    @ColumnType(jdbcType = JDBCType.TIMESTAMP)
    @DefaultValue(generator = Generators.CURRENT_TIME)//逻辑默认值
    @Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY)
    private Date createTime;

    @Column
    @ColumnType(jdbcType = JDBCType.TIMESTAMP)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    @Schema(description = "更新时间")
    private Date updateTime;

    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    @JsonCodec
    private Map<String, Object> configuration;

    public ProtocolSupportDefinition toUnDeployDefinition() {
        ProtocolSupportDefinition definition = toDeployDefinition();
        definition.setState((byte) 0);
        return definition;
    }

    public ProtocolSupportDefinition toDeployDefinition() {
        ProtocolSupportDefinition definition = new ProtocolSupportDefinition();
        definition.setId(getId());
        definition.setConfiguration(configuration);
        definition.setName(name);
        definition.setProvider(type);
        definition.setState((byte) 1);

        return definition;
    }
}
