/**
 * llkang.com Inc.
 * Copyright (c) 2010-2022 All Rights Reserved.
 */
package top.iot.gateway.network;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 设备缓存信息
 *
 * @author kanglele
 * @version $Id: DeviceCacheMessage, v 0.1 2022/9/8 13:50 kanglele Exp $
 */
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@Data
public class DeviceCacheMessage implements Serializable {

    public static final transient String INDEX = "device:";

    @JsonProperty("deviceId")
    private String deviceId;
    /**
     * 设备类型
     */
    @JsonProperty("deviceType")
    private String deviceType;
    /**
     * 是否调试(0:否 1:是)
     */
    @JsonProperty("testFlag")
    private Integer testFlag;
    @JsonProperty("metadata")
    private MetadataDTO metadata;
    /**
     * 产品code
     */
    @JsonProperty("productId")
    private String productId;
    /**
     * 父设备id
     */
    @JsonProperty("parentGatewayId")
    private String parentGatewayId;
    @JsonProperty("selfManageState")
    private Boolean selfManageState;
    /**
     * 状态(-3:未激活,-2:状态检查超时,-1:离线,0:未知,1:在线)
     */
    @JsonProperty("state")
    private Integer state;
    @JsonProperty("shadow")
    private String shadow;
    @JsonProperty("username")
    private String username;
    @JsonProperty("password")
    private String password;
    /**
     * 租户
     */
    @JsonProperty("tenantId")
    private String tenantId;

    @NoArgsConstructor
    @Data
    public static class MetadataDTO {
        @JsonProperty("measureProperties")
        private List<MeasurePropertiesDTO> measureProperties;

        @NoArgsConstructor
        @Data
        public static class MeasurePropertiesDTO {
            @JsonProperty("note")
            private String note;
            @JsonProperty("dataDefinitions")
            private List<Object> dataDefinitions;
            @JsonProperty("code")
            private String code;
            @JsonProperty("max")
            private String max;
            @JsonProperty("calcRule")
            private String calcRule;
            @JsonProperty("readOnly")
            private Boolean readOnly;
            @JsonProperty("nameEn")
            private String nameEn;
            @JsonProperty("type")
            private String type;
            @JsonProperty("deviceId")
            private String deviceId;
            @JsonProperty("unit")
            private String unit;
            @JsonProperty("min")
            private String min;
            @JsonProperty("valueType")
            private String valueType;
            @JsonProperty("name")
            private String name;
            @JsonProperty("isVirtual")
            private Boolean isVirtual;
        }
    }
}
