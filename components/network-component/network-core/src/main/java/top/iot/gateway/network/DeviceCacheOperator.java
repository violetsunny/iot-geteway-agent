/**
 * llkang.com Inc.
 * Copyright (c) 2010-2022 All Rights Reserved.
 */
package top.iot.gateway.network;

import lombok.extern.slf4j.Slf4j;
import top.iot.gateway.core.config.ConfigStorage;
import top.iot.gateway.core.config.ConfigStorageManager;
import top.iot.gateway.core.config.StorageConfigurable;
import reactor.core.publisher.Mono;

/**
 * 操作设备缓存
 *
 * @author kanglele
 * @version $Id: DeviceCacheOperator, v 0.1 2022/9/15 17:34 kanglele Exp $
 */
@Slf4j
public class DeviceCacheOperator implements StorageConfigurable {

    private final Mono<ConfigStorage> storageMono;

    public DeviceCacheOperator(String cacheKey,ConfigStorageManager manager) {
        this.storageMono = manager.getStorage(cacheKey).switchIfEmpty(Mono.fromRunnable(() -> log.warn("[{}] not fond", cacheKey)));
    }

    @Override
    public Mono<ConfigStorage> getReactiveStorage() {
        return storageMono;
    }
}
