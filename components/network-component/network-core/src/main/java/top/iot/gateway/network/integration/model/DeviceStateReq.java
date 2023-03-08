/**
 * llkang.com Inc.
 * Copyright (c) 2010-2022 All Rights Reserved.
 */
package top.iot.gateway.network.integration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 设备状态
 *
 * @author kanglele
 * @version $Id: DeviceStateReq, v 0.1 2022/9/6 16:52 kanglele Exp $
 */
@AllArgsConstructor(staticName = "of")
@NoArgsConstructor
@Data
public class DeviceStateReq implements Serializable {
    /**
     * [deviceId1,deviceId]
     */
    @JsonProperty("deviceIds")
    private List<String> deviceIds;
    /**
     * 状态(-3:未激活,-2:状态检查超时,-1:离线,0:未知,1:在线)
     */
    @JsonProperty("state")
    private Integer state;
    /**
     * 更新标识, 1更新设备, 2更新子设备
     */
    @JsonProperty("flag")
    private String flag;

}
