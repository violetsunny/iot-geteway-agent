/**
 * llkang.com Inc.
 * Copyright (c) 2010-2022 All Rights Reserved.
 */
package top.iot.gateway.network.integration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备简要信息
 *
 * @author kanglele
 * @version $Id: DeviceInfo, v 0.1 2022/8/22 16:10 kanglele Exp $
 */
@NoArgsConstructor
@Data
public class DeviceDataRes {


    @JsonProperty("code")
    private Integer code;
    @JsonProperty("success")
    private Boolean success;
    @JsonProperty("data")
    private DeviceData data;
    @JsonProperty("msg")
    private String msg;

    @NoArgsConstructor
    @Data
    public static class DeviceData {
        @JsonProperty("id")
        private String id;
        @JsonProperty("sn")
        private String sn;
        @JsonProperty("name")
        private String name;
        @JsonProperty("cloudCode")
        private String cloudCode;
        @JsonProperty("productId")
        private String productId;
        @JsonProperty("productCode")
        private String productCode;
        @JsonProperty("productName")
        private String productName;
        @JsonProperty("categoryCode")
        private String categoryCode;
        @JsonProperty("categoryName")
        private String categoryName;
        @JsonProperty("brandCode")
        private String brandCode;
        @JsonProperty("brandName")
        private String brandName;
        @JsonProperty("brandModelCode")
        private String brandModelCode;
        @JsonProperty("brandModelName")
        private String brandModelName;
        @JsonProperty("state")
        private Integer state;
        @JsonProperty("onlineState")
        private Integer onlineState;
        @JsonProperty("testFlag")
        private Integer testFlag;
        @JsonProperty("registryTime")
        private String registryTime;
        @JsonProperty("photoUrl")
        private String photoUrl;
        @JsonProperty("address")
        private String address;
        @JsonProperty("longitude")
        private String longitude;
        @JsonProperty("latitude")
        private String latitude;
        @JsonProperty("altitude")
        private String altitude;
//        @JsonProperty("productProperties")
//        private List<?> productProperties;
//        @JsonProperty("inherentProperties")
//        private List<?> inherentProperties;
//        @JsonProperty("tags")
//        private List<?> tags;
//        @JsonProperty("configs")
//        private ConfigsDTO configs;
        @JsonProperty("description")
        private String description;
        @JsonProperty("plateUrl")
        private String plateUrl;
        @JsonProperty("distantViewUrl")
        private String distantViewUrl;
        @JsonProperty("createTime")
        private String createTime;
        @JsonProperty("ckInstanceId")
        private String ckInstanceId;
        @JsonProperty("entityTypeId")
        private String entityTypeId;
        @JsonProperty("entityTypeCode")
        private String entityTypeCode;
        @JsonProperty("entityTypeSource")
        private String entityTypeSource;
        @JsonProperty("deviceType")
        private String deviceType;
        @JsonProperty("gatewayDeviceFlag")
        private Boolean gatewayDeviceFlag;
        @JsonProperty("updateUser")
        private String updateUser;
        @JsonProperty("updateTime")
        private String updateTime;
        @JsonProperty("isDeleted")
        private Integer isDeleted;
        @JsonProperty("createUser")
        private String createUser;
        @JsonProperty("parentId")
        private String parentId;
        @JsonProperty("entId")
        private String entId;
        @JsonProperty("tenantId")
        private String tenantId;
    }
}
