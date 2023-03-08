package top.iot.gateway.network.mqtt.server;

import top.iot.gateway.network.Network;
import reactor.core.publisher.Flux;

/**
 * MQTT服务端
 *
 * @author hanyl
 * @version 1.0
 * @since 1.0
 */
public interface MqttServer extends Network {

    /**
     * 订阅客户端连接
     *
     * @return 客户端连接流
     */
    Flux<MqttConnection> handleConnection();

}
