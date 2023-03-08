package top.iot.gateway.network.coap.server;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.OptionSet;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.DelivererException;
import org.eclipse.californium.core.server.ServerMessageDeliverer;
import org.eclipse.californium.core.server.resources.Resource;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.List;
import java.util.function.Function;

@Slf4j
public class CoapServerMessageDeliverer extends ServerMessageDeliverer {

    private EmitterProcessor<CoapConnection> processor = EmitterProcessor.create(false);

    FluxSink<CoapConnection> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);

    public CoapServerMessageDeliverer(Resource root) {
        super(root);
    }

    @Override
    protected boolean preDeliverRequest(Exchange exchange) {
        if (!processor.hasDownstreams()) {
            log.info("coap server no handler for:[{}]", exchange.getRequest().getSourceContext().getPeerAddress().getHostString());
            exchange.sendReject();
            return true;
        }
        sink.next(new DefaultCoapConnection(exchange, this));
        return super.preDeliverRequest(exchange);
    }

    public Flux<CoapConnection> handleConnection() {
        return processor.map(Function.identity());
    }

    @SneakyThrows
    @Override
    protected Resource findResource(Exchange exchange) {
        validateUriPath(exchange);
        return findResource(exchange.getRequest().getOptions().getUriPath());
    }

    private void validateUriPath(Exchange exchange) {
        OptionSet options = exchange.getRequest().getOptions();
        List<String> uriPathList = options.getUriPath();
        String path = toPath(uriPathList);
        if (path != null) {
            options.setUriPath(path);
            exchange.getRequest().setOptions(options);
        }
    }

    private String toPath(List<String> list) {
        if (!CollectionUtils.isEmpty(list) && list.size() == 1) {
            final String slash = "/";
            String path = list.get(0);
            if (path.startsWith(slash)) {
                path = path.substring(slash.length());
            }
            return path;
        }
        return null;
    }
}
