/**
 * llkang.com Inc.
 * Copyright (c) 2010-2022 All Rights Reserved.
 */
package top.iot.gateway.network;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.iot.gateway.core.config.ConfigKey;

/**
 * 设备缓存key
 *
 * @author kanglele
 * @version $Id: DeviceMessageConfigKey, v 0.1 2022/9/8 16:12 kanglele Exp $
 */
@AllArgsConstructor
@Getter
public enum DeviceMessageConfigKey implements ConfigKey<String> {

    metadata("物模型"),
    /**
     * 产品code
     */
    productId("产品ID"),

    protocol("消息协议"),

    parentGatewayId("上级网关设备ID"),

    sessionId("设备会话ID"),

    shadow("设备影子"),

    /**
     * 设备类型
     * device: 直连设备
     * childrenDevice: 网关子设备
     * gateway: 网关设备
     */
    deviceType("设备类型"),

    deviceName("设备名称"),

    period("上报周期"),

    password("密码"),

    username("用户名"),

    tenantId("租户id"),

    sn("sn"),
    ;

    final String name;

    public static ConfigKey<Boolean> isGatewayDevice = ConfigKey.of("isGatewayDevice", "是否为网关设备");
    //通常用于子设备状态
    public static ConfigKey<Boolean> selfManageState = ConfigKey.of("selfManageState", "状态自管理");
    /**
     * 状态(-3:未激活,-2:状态检查超时,-1:离线,0:未知,1:在线)
     */
    public static ConfigKey<Integer> state = ConfigKey.of("state", "设备状态");
    /**
     * 0:否 1:是
     */
    public static ConfigKey<Integer> testFlag = ConfigKey.of("testFlag", "是否调试");
    /**
     * value一般是设备id
     */
    public static ConfigKey<Integer> tcp_auth_key = ConfigKey.of("tcp_auth_key", "tcp连接身份key");


    @Override
    public String getKey() {
        return name();
    }

    @Override
    public Class<String> getType() {
        return String.class;
    }

}
