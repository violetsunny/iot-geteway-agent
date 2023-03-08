package top.iot.gateway.network;

import top.iot.gateway.core.config.ConfigStorage;
import top.iot.gateway.core.config.ConfigStorageManager;
import top.iot.gateway.core.config.StorageConfigurable;
import reactor.core.publisher.Mono;


public class SessionIdDeviceIdBindingRegistry implements StorageConfigurable {

    private final Mono<ConfigStorage> storageMono;

    public SessionIdDeviceIdBindingRegistry(ConfigStorageManager manager) {
        this.storageMono = manager.getStorage("client-deviceId-binding");
    }

    @Override
    public Mono<ConfigStorage> getReactiveStorage() {
        return storageMono;
    }
}
