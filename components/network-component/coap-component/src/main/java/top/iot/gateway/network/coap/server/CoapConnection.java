package top.iot.gateway.network.coap.server;

import org.eclipse.californium.core.network.Exchange;
import top.iot.gateway.core.message.codec.CoapMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Consumer;

public interface CoapConnection {

    String getClientId();

    Exchange getExchange();

    void reject();

    void onClose(Consumer<CoapConnection> listener);

    Mono<Void> close();

    Flux<CoapMessage> handleMessage();

    boolean isAlive();

    void keepAlive();

}
