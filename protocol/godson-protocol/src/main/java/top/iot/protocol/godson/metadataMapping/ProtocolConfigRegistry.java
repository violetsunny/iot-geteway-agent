package top.iot.protocol.godson.metadataMapping;

import top.iot.gateway.core.cache.Caches;
import top.iot.gateway.core.config.ConfigStorage;
import top.iot.gateway.core.config.ConfigStorageManager;
import top.iot.gateway.core.message.codec.MessagePayloadType;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ProtocolConfigRegistry {
    private final ConfigStorageManager manager;

    private final MessagePayloadParserBuilder messagePayloadParserBuilder;



    private final Map<String, DeviceProtocolConfigOperator> protocolConfigOperatorMap = Caches.newCache();

    public ProtocolConfigRegistry(ConfigStorageManager manager, MessagePayloadParserBuilder messagePayloadParserBuilder) {
        this.manager = manager;
        this.messagePayloadParserBuilder = messagePayloadParserBuilder;
    }

    public Mono<DeviceProtocolConfigOperator> register(ProtocolConfigInfo protocolConfigInfo) {
        return Mono.defer(() -> {
            DefaultDeviceProtocolConfigOperator operator = createProtocolConfigOperator(protocolConfigInfo.getId());
            protocolConfigOperatorMap.put(operator.getId(), operator);
            Map<String, Object> configs = new HashMap<>();
            Optional.ofNullable(protocolConfigInfo.getMetadataMapping())
                .ifPresent(conf -> configs.put("metadataMapping", conf));
            // metadataMapping 放入缓存 key:device-protocol:${productId}
            return operator.setConfigs(configs)
                .thenReturn(operator);
        });
    }

    public Mono<Void> unregister(String protocolConfigId) {
        return this
            .getProtocolConfig(protocolConfigId)
            .then(
                manager.getStorage("device-protocol:" + protocolConfigId)
                    .flatMap(ConfigStorage::clear)
                    .doOnSuccess(r -> protocolConfigOperatorMap.remove(protocolConfigId))
            )
            .then();
    }

    public Mono<DeviceProtocolConfigOperator> getProtocolConfig(String protocolConfigId) {
        if (StringUtils.isEmpty(protocolConfigId)) {
            return Mono.empty();
        }
        {
            DeviceProtocolConfigOperator operator = protocolConfigOperatorMap.get(protocolConfigId);
            if (null != operator) {
                return Mono.just(operator);
            }
        }
        DeviceProtocolConfigOperator protocolConfigOperator = createProtocolConfigOperator(protocolConfigId);
        protocolConfigOperatorMap.put(protocolConfigId, protocolConfigOperator);
        return Mono.justOrEmpty(protocolConfigOperator);
    }

    private DefaultDeviceProtocolConfigOperator createProtocolConfigOperator(String protocolConfigId) {
        return new DefaultDeviceProtocolConfigOperator(protocolConfigId, manager);
    }

    public Mono<DeviceMetadataMapping> getMetadataMapping(Mono<String> protocolConfigId) {
        return protocolConfigId.flatMap(id ->
            getProtocolConfig(id).flatMap(DeviceProtocolConfigOperator::getMetadataMapping));
    }

    public MessagePayloadParser getMessagePayloadParser(String messageType) {
        return getMessagePayloadParser(MessagePayloadType.valueOf(messageType));
    }

    public MessagePayloadParser getMessagePayloadParser(MessagePayloadType messageType) {
        return messagePayloadParserBuilder.build(messageType);
    }
}
