package top.iot.gateway.network.http.client;

import top.iot.gateway.network.Network;
import top.iot.gateway.core.message.codec.http.HttpRequestMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;

public interface HttpClient extends Network {

    /**
     * 获取客户端远程地址
     * @return 客户端远程地址
     */
    InetSocketAddress getRemoteAddress();

    /**
     * 订阅HTTP消息,
     *
     * @return HTTP消息
     */
    Flux<HttpRequestMessage> subscribe();

    /**
     * 向客户端发送数据
     * @param message 数据对象
     * @return 发送结果
     */
    Mono<Boolean> send(HttpRequestMessage message);

    void onDisconnect(Runnable disconnected);

    /**
     * 连接保活
     */
    void keepAlive();

    /**
     * 设置客户端心跳超时时间
     *
     * @param timeout 超时时间
     */
    void setKeepAliveTimeout(Duration timeout);

    /**
     * 重置
     */
    void reset();


    void received(HttpRequestMessage message);
}
