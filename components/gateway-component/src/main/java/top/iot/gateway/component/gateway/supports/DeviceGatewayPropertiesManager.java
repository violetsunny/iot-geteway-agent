package top.iot.gateway.component.gateway.supports;

import reactor.core.publisher.Mono;

/**
 * 设备网关属性管理器
 *
 * @author hanyl
 */
public interface DeviceGatewayPropertiesManager {

    /**
     * 获取网关的属性
     *
     * @param id 网关ID
     * @return 网关属性
     */
    Mono<DeviceGatewayProperties> getProperties(String id);


}
