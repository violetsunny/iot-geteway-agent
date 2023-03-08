/**
 * llkang.com Inc.
 * Copyright (c) 2010-2022 All Rights Reserved.
 */
package top.iot.gateway.network.integration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author kanglele
 * @version $Id: DeviceStateRes, v 0.1 2022/9/6 16:54 kanglele Exp $
 */
@NoArgsConstructor
@Data
public class DeviceStateRes implements Serializable {
    /**
     * 200成功
     */
    @JsonProperty("code")
    private Integer code;
    @JsonProperty("success")
    private Boolean success;
    @JsonProperty("data")
    private Object data;
    @JsonProperty("msg")
    private String msg;

}
