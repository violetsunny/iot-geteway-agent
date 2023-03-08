package top.iot.gateway.network.coap.server;

import top.iot.gateway.network.coap.device.CoapTransportResource;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.network.Exchange;
import top.iot.gateway.core.message.codec.CoapMessage;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Consumer;

@Slf4j
public class DefaultCoapConnection implements CoapConnection {

    private volatile boolean closed = false;

    @Getter
    private Exchange exchange;

    private CoapServerMessageDeliverer deliverer;

    private final EmitterProcessor<CoapMessage> messageProcessor = EmitterProcessor.create(false);
    private final FluxSink<CoapMessage> sink = messageProcessor.sink(FluxSink.OverflowStrategy.BUFFER);
    private final EmitterProcessor<CoapMessage> subscription = EmitterProcessor.create(false);
    private final EmitterProcessor<CoapMessage> unsubscription = EmitterProcessor.create(false);

    @Setter
//    private long keepAliveTimeoutMs = Duration.ofMinutes(10).toMillis();
    private long keepAliveTimeoutMs = Duration.ofSeconds(120).toMillis();
    private volatile long lastKeepAliveTime = System.currentTimeMillis();

    public DefaultCoapConnection(Exchange exchange, CoapServerMessageDeliverer deliverer) {
        this.exchange = exchange;
        this.deliverer = deliverer;
    }

    @Override
    public void reject() {
        if (closed) {
            return;
        }
        exchange.sendReject();
        complete();
    }

    @Override
    public void onClose(Consumer<CoapConnection> listener) {
        disconnectConsumer = disconnectConsumer.andThen(listener);
    }

    @Override
    public Mono<Void> close() {
        return Mono.<Void>fromRunnable(() -> {
//            Endpoint endpoint = exchange.getEndpoint();

        }).doFinally(s -> this.complete());
    }

    private void complete() {
        if (closed) {
            return;
        }
        closed = true;
        disconnectConsumer.accept(this);
        disconnectConsumer = defaultListener;
    }

    private final Consumer<CoapConnection> defaultListener = coapConnection -> {
        log.debug("coap client [{}] disconnected", getClientId());
        subscription.onComplete();
        unsubscription.onComplete();
        messageProcessor.onComplete();
    };

    private Consumer<CoapConnection> disconnectConsumer = defaultListener;

    @Override
    public String getClientId() {
        return exchange.getRequest().getSourceContext().getPeerAddress().getHostString();
    }

    @Override
    public Flux<CoapMessage> handleMessage() {
        return ((CoapTransportResource)deliverer.findResource(exchange)).handleRequest();
    }

    @Override
    public boolean isAlive() {
        return keepAliveTimeoutMs < 0 || System.currentTimeMillis() - lastKeepAliveTime < keepAliveTimeoutMs;
    }

    @Override
    public void keepAlive() {
        lastKeepAliveTime = System.currentTimeMillis();
    }
}
