package top.iot.gateway.manager.entity;

import top.iot.gateway.network.NetworkProperties;
import top.iot.gateway.network.NetworkType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import top.iot.gateway.manager.enums.NetworkConfigState;
import org.hswebframework.web.crud.generator.Generators;

import javax.persistence.Column;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.sql.JDBCType;
import java.util.Date;
import java.util.Map;

@Getter
@Setter
@Table(name = "network_config")
public class NetworkConfigEntity extends GenericEntity<String> {

    @Column
    @NotNull(message = "名称不能为空")
    @Schema(description = "名称")
    private String name;

    @Column
    @Schema(description = "服务器ID")
    private String serverId;

    @Column
    @Schema(description = "说明")
    private String description;

    @Column(nullable = false)
    @NotNull(message = "类型不能为空")
    private String type;

    @Column(nullable = false)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("disabled")
    @Schema(description = "状态")
    private NetworkConfigState state;

    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    @Schema(description = "配置(根据类型不同而不同)")
    private Map<String, Object> configuration;

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

    public NetworkType lookupNetworkType() {
        return NetworkType.lookup(type).orElseGet(() -> NetworkType.of(type));
    }

    public NetworkProperties toNetworkProperties() {
        NetworkProperties properties = new NetworkProperties();
        properties.setConfigurations(configuration);
        properties.setEnabled(state == NetworkConfigState.enabled);
        properties.setId(getId());
        properties.setName(name);
        properties.setServerId(serverId);

        return properties;
    }

}
