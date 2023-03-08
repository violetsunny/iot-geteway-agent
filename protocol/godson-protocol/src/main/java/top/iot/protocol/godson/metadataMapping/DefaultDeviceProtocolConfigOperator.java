package top.iot.protocol.godson.metadataMapping;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import top.iot.gateway.core.config.ConfigKey;
import top.iot.gateway.core.config.ConfigStorage;
import top.iot.gateway.core.config.ConfigStorageManager;
import top.iot.gateway.core.config.StorageConfigurable;
import reactor.core.publisher.Mono;

import java.util.Map;


public class DefaultDeviceProtocolConfigOperator implements DeviceProtocolConfigOperator, StorageConfigurable {

    @Getter
    private final String id;

    private volatile DeviceMetadataMapping metadataMapping;

    private final Mono<ConfigStorage> storageMono;

    private long lstMetadataMappingChangeTime;

    private static final ConfigKey<Long> lastMetadataMappingTimeKey = ConfigKey.of("lst_metadata_mapping_time");

    private final Mono<DeviceMetadataMapping> inLocalMetadataMapping;

    private final Mono<DeviceMetadataMapping> metadataMappingMono;

    public DefaultDeviceProtocolConfigOperator(String id, ConfigStorageManager manager) {
        this.id = id;
        this.storageMono = manager.getStorage("device-protocol:".concat(id));
        this.inLocalMetadataMapping = Mono.fromSupplier(() -> metadataMapping);

        Mono<DeviceMetadataMapping> loadMetadataMapping = Mono
            .zip(
                this.getConfig("metadataMapping"),
                this.getConfig(lastMetadataMappingTimeKey)
                    .switchIfEmpty(Mono.defer(() -> {
                        long now = System.currentTimeMillis();
                        return this
                            .setConfig(lastMetadataMappingTimeKey, now)
                            .thenReturn(now);
                    }))
            )
            .flatMap(tp2 ->
                Mono.just(new GatewayDeviceMetadataMapping(JSON.parseObject(tp2.getT1().asString())))
                .doOnNext(decode -> {
                    this.metadataMapping = decode;
                    this.lstMetadataMappingChangeTime = tp2.getT2();
                }));
        this.metadataMappingMono = Mono
            .zip(
                inLocalMetadataMapping,
                getConfig(lastMetadataMappingTimeKey)
            )
            .flatMap(tp2 -> {
                if (tp2.getT2().equals(lstMetadataMappingChangeTime)) {
                    return inLocalMetadataMapping;
                }
                return Mono.empty();
            })
            .switchIfEmpty(loadMetadataMapping);
    }

    @Override
    public Mono<ConfigStorage> getReactiveStorage() {
        return storageMono;
    }


    @Override
    public Mono<DeviceMetadataMapping> getMetadataMapping() {
        return metadataMappingMono;
    }

    @Override
    public Mono<Boolean> setConfigs(Map<String, Object> conf) {
        if (conf.containsKey("metadataMapping")) {
            conf.put(lastMetadataMappingTimeKey.getKey(), System.currentTimeMillis());
        }
        return StorageConfigurable.super.setConfigs(conf);
    }

}

