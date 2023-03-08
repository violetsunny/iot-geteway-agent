package top.iot.protocol.godson.metadataMapping;

import top.iot.gateway.core.Configurable;
import reactor.core.publisher.Mono;

/**
 * 协议配置操作
 *
 */
public interface DeviceProtocolConfigOperator extends Configurable {

    /**
     * @return 协议配置信息
     */
    Mono<DeviceMetadataMapping> getMetadataMapping();

}
