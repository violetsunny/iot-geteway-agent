package top.iot.gateway.network.coap.device;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import top.iot.gateway.core.message.codec.CoapExchangeMessage;
import top.iot.gateway.core.message.codec.CoapMessage;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.function.Function;

@Slf4j
public class CoapTransportResource extends CoapResource {

    private EmitterProcessor<CoapMessage> messageProcessor = EmitterProcessor.create(false);

    FluxSink<CoapMessage> sink = messageProcessor.sink(FluxSink.OverflowStrategy.BUFFER);

    public CoapTransportResource(String name) {
        super(name);
        this.setObservable(true); // enable observing
    }

    /*
     * Overwritten method from CoapResource to be able to manage our own observe notification counters.
     */
    @Override
    public void checkObserveRelation(Exchange exchange, Response response) {
       super.checkObserveRelation(exchange, response);
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        super.handleGET(exchange);
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        if(!messageProcessor.hasDownstreams()){
            exchange.reject();
        }
        exchange.accept();
        sink.next(new CoapExchangeMessage(exchange));
    }

    public Flux<CoapMessage> handleRequest() {
        return messageProcessor.map(Function.identity());
    }

    @Override
    public Resource getChild(String name) {
        return this;
    }


}
