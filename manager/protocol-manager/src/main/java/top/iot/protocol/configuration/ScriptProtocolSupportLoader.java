package top.iot.protocol.configuration;

import lombok.extern.slf4j.Slf4j;
import top.iot.gateway.core.ProtocolSupport;
import top.iot.gateway.core.spi.ProtocolSupportProvider;
import top.iot.gateway.core.spi.ServiceContext;
import top.iot.gateway.core.trace.MonoTracer;
import top.iot.gateway.core.trace.ProtocolTracer;
import top.iot.gateway.supports.protocol.management.ProtocolSupportDefinition;
import top.iot.gateway.supports.protocol.management.ProtocolSupportLoaderProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@Slf4j
public class ScriptProtocolSupportLoader implements ProtocolSupportLoaderProvider {

    @Autowired
    private ServiceContext serviceContext;

    @Override
    public String getProvider() {
        return "script";
    }

    @Override
    public Mono<? extends ProtocolSupport> load(ProtocolSupportDefinition definition) {
        String id = definition.getId();
        return Mono
                .defer(() -> {
                    try {
                        ProtocolSupportProvider supportProvider = new ScriptProtocolSupportProvider(definition);
                        return supportProvider
                                .create(serviceContext)
                                .onErrorMap(Exceptions::bubble);
                    } catch (Throwable e) {
                        return Mono.error(e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .as(MonoTracer.create(ProtocolTracer.SpanName.install(id)));
    }
}
