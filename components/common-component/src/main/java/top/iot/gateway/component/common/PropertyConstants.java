package top.iot.gateway.component.common;

import top.iot.gateway.core.config.ConfigKey;
import top.iot.gateway.core.message.HeaderKey;

import java.util.Map;
import java.util.Optional;

/**
 * @author wangzheng
 * @since 1.0
 */
public interface PropertyConstants {
    Key<String> orgId = Key.of("orgId");

    Key<String> deviceName = Key.of("deviceName");

    Key<String> productId = Key.of("productId");

    Key<String> uid = Key.of("_uid");


    @SuppressWarnings("all")
    static <T> Optional<T> getFromMap(ConfigKey<T> key, Map<String, Object> map) {
        return Optional.ofNullable((T) map.get(key.getKey()));
    }

    interface Key<V> extends ConfigKey<V>/*, HeaderKey<V>*/ {

        @Override
        default Class<V> getType() {
            return ConfigKey.super.getType();
        }

        static <T> Key<T> of(String key) {
            return new Key<T>() {
                @Override
                public String getKey() {
                    return key;
                }
                //@Override
                public T getDefaultValue() {
                    return null;
                }
            };
        }

    }
}
