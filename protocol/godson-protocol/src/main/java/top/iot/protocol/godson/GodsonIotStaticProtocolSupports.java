package top.iot.protocol.godson;

import top.iot.protocol.godson.metadataMapping.ProtocolConfigRegistry;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import top.iot.gateway.core.ProtocolSupport;
import top.iot.gateway.core.ProtocolSupports;
import top.iot.gateway.core.spi.ServiceContext;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class GodsonIotStaticProtocolSupports implements ProtocolSupports {
    @Setter
    private ServiceContext serviceContext;
    @Setter
    private ProtocolConfigRegistry registry;

    private Map<String, ProtocolSupport> supports = new ConcurrentHashMap<>();

    @Override
    public boolean isSupport(String protocol) {
        return supports.containsKey(protocol);
    }

    @Override
    public Mono<ProtocolSupport> getProtocol(String protocol) {
        ProtocolSupport support = supports.get(protocol);
        if (support == null) {
            return Mono.error(new UnsupportedOperationException("不支持的协议 :" + protocol));
        }
        return Mono.just(support);
    }

    @Override
    public Flux<ProtocolSupport> getProtocols() {
        return Flux.fromIterable(supports.values());
    }

    public void init() {
        new GodsonIotProtocolSupportProvider(registry).create(serviceContext).subscribe(p -> {
            supports.put(p.getId(), p);
        });
    }
}
