package top.iot.protocol.godson.conf;

import top.iot.protocol.godson.GodsonIotStaticProtocolSupports;
import top.iot.protocol.godson.metadataMapping.MessagePayloadParserBuilder;
import top.iot.protocol.godson.metadataMapping.ProtocolConfigRegistry;
import top.iot.gateway.core.config.ConfigStorageManager;
import top.iot.gateway.core.spi.ServiceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProtocolConfiguration {
    @Bean
    public MessagePayloadParserBuilder messagePayloadParserBuilder() {
        return new MessagePayloadParserBuilder();
    }

    @Bean
    public ProtocolConfigRegistry protocolConfigRegistry(ConfigStorageManager manager,
                                                         MessagePayloadParserBuilder messagePayloadParserBuilder) {
        return new ProtocolConfigRegistry(manager, messagePayloadParserBuilder);
    }

    @Bean(initMethod = "init")
    public GodsonIotStaticProtocolSupports godsonIotStaticProtocolSupports(ServiceContext serviceContext, ProtocolConfigRegistry registry) {
        GodsonIotStaticProtocolSupports supports = new GodsonIotStaticProtocolSupports();
        supports.setServiceContext(serviceContext);
        supports.setRegistry(registry);
        return supports;
    }
}
