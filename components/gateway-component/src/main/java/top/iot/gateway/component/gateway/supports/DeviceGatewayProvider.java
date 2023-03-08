package top.iot.gateway.component.gateway.supports;

import top.iot.gateway.component.gateway.DeviceGateway;
import top.iot.gateway.network.NetworkType;
import reactor.core.publisher.Mono;

/**
 * 设备网关支持提供商,用于提供对各种设备网关的支持.在启动设备网关时,会根据对应的提供商以及配置来创建设备网关.
 * 实现统一管理网关配置,动态创建设备网关.
 *
 * @author hanyl
 * @see DeviceGateway
 * @since 1.0
 */
public interface DeviceGatewayProvider {

    String getId();

    String getName();

    NetworkType getNetworkType();

    Mono<DeviceGateway> createDeviceGateway(DeviceGatewayProperties properties);

}
