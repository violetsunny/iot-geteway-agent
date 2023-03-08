package top.iot.gateway.manager.entity;

import top.iot.gateway.manager.enums.NetworkConfigState;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.*;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.generator.Generators;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;
import java.util.Date;
import java.util.Map;

/**
 * @author wangzheng
 * @see
 * @since 1.0
 */
@Getter
@Setter
@Table(name = "device_gateway")
public class DeviceGatewayEntity extends GenericEntity<String> {

    @Comment("名称")
    @Column
    @Schema(description = "名称")
    private String name;

    @Column
    @Schema(description = "服务器ID")
    private String serverId;

    @Comment("类型")
    @Column
    @Schema(description = "网关类型")
    private String provider;

    @Column
    @EnumCodec
    @ColumnType(javaType = String.class)
    @Schema(description = "状态")
    private NetworkConfigState state;

    @Comment("网络组件id")
    @Column(name = "network_id", length = 64)
    @Hidden
    private String networkId;

    // 根据provider选择 协议  根据协议选择网络组件
//    @Comment("支持的协议")
//    @Column
//    @Schema(description = "支持的协议")
//    private String protocol;

    @Comment("其他配置")
    @Column
    @ColumnType(jdbcType = JDBCType.CLOB)
    @JsonCodec
    @Schema(description = "配置信息(根据类型不同而不同)")
    private Map<String,Object> configuration;

    @Comment("描述")
    @Column
    @Schema(description = "说明")
    private String describe;

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

}
