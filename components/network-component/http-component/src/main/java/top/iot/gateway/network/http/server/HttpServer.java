package top.iot.gateway.network.http.server;

import top.iot.gateway.network.Network;
import top.iot.gateway.network.http.client.HttpClient;
import reactor.core.publisher.Flux;

/**
 * HTTP服务
 **/
public interface HttpServer extends Network {

    /**
     * 订阅客户端连接
     *
     * @return 客户端流
     * @see HttpClient
     */
    Flux<HttpClient> handleConnection();

    /**
     * 关闭服务端
     */
    void shutdown();

}
