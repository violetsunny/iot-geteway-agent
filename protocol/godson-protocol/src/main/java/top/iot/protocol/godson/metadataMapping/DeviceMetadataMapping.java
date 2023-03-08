package top.iot.protocol.godson.metadataMapping;

import top.iot.gateway.core.metadata.Jsonable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DeviceMetadataMapping extends Jsonable {
    List<MetadataMapping> getProperties();

    List<MetadataMapping> getEvents();

    default Optional<MetadataMapping> getEvent(String id) {
        return Optional.ofNullable(getEventOrNull(id));
    }

    MetadataMapping getEventOrNull(String id);

    default Optional<MetadataMapping> getProperty(String id) {
        return Optional.ofNullable(getPropertyOrNull(id));
    }

    MetadataMapping getPropertyOrNull(String id);

    Map<String, Object> getMsgInfo();

    default Optional<Object> getMsgInfo(String name) {
        return Optional.ofNullable(this.getMsgInfo()).map((map) -> {
            return map.get(name);
        });
    }

    Map<String, Object> getMsgType();

    default Optional<Object> getMsgType(String name) {
        return Optional.ofNullable(this.getMsgType()).map((map) -> {
            return map.get(name);
        });
    }

    String getType();
}

