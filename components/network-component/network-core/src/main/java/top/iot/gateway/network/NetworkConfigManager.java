package top.iot.gateway.network;

import reactor.core.publisher.Mono;

/**
 * 网络组件配置管理器
 *
 * @author hanyl
 */
public interface NetworkConfigManager {

    Mono<NetworkProperties> getConfig(NetworkType networkType, String id);

}
