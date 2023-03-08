/**
 * llkang.com Inc.
 * Copyright (c) 2010-2022 All Rights Reserved.
 */
package top.iot.gateway.network.integration;

import top.iot.gateway.network.integration.model.DeviceDataRes;
import top.iot.gateway.network.integration.model.DeviceStateReq;
import top.iot.gateway.network.integration.model.DeviceStateRes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 设备管理
 *
 * @author kanglele
 * @version $Id: DeviceClient, v 0.1 2022/8/22 16:03 kanglele Exp $
 */
@FeignClient(url = "${top.iot.device.url}", name = "iot-device")
public interface DeviceClient {

    @GetMapping(value = "/device/get/{deviceId}")
    DeviceDataRes getDeviceId(@PathVariable(value = "deviceId") String deviceId);

    @PostMapping(value = "/device/batchUpdateDeviceState")
    DeviceStateRes batchUpdateDeviceState(@RequestBody DeviceStateReq req);

}
