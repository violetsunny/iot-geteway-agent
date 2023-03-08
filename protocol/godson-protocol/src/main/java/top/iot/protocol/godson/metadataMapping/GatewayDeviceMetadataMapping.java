package top.iot.protocol.godson.metadataMapping;

import com.alibaba.fastjson.JSONObject;
import lombok.NoArgsConstructor;
import top.iot.gateway.core.metadata.DataType;
import top.iot.gateway.core.metadata.Jsonable;
import top.iot.gateway.core.metadata.types.ObjectType;
import top.iot.gateway.supports.official.IotGatewayDeviceMetadata;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@NoArgsConstructor
public class GatewayDeviceMetadataMapping implements DeviceMetadataMapping {
    private JSONObject jsonObject;

    private volatile Map<String, MetadataMapping> properties;
    private volatile Map<String, MetadataMapping> events;

    private String type;

    public GatewayDeviceMetadataMapping(IotGatewayDeviceMetadata gatewayDeviceMetadata, DeviceMetadataMapping another) {
        Map<String, MetadataMapping> propertyMap = Optional.ofNullable(another.getProperties())
            .map(Collection::stream)
            .map(stream -> stream.collect(Collectors.toMap(MetadataMapping::getId, Function.identity()))).orElse(Collections.emptyMap());
        properties = Optional.ofNullable(gatewayDeviceMetadata.getProperties())
            .map(Collection::stream)
            .map(stream -> stream.map(propertyMetadata -> {
                DataType valueType = propertyMetadata.getValueType();
                MetadataMapping metadataMapping = propertyMap.get(propertyMetadata.getId());
                if (metadataMapping==null || !valueType.getType().equals(metadataMapping.getType()) && valueType instanceof ObjectType) {
                    return new GatewayMetadataMapping(propertyMetadata, another.getType());
                }

                if (!propertyMetadata.getName().equals(metadataMapping.getName())) {
                    metadataMapping.setName(propertyMetadata.getName());
                }
                if (!valueType.getType().equals(metadataMapping.getType())) {
                    metadataMapping.setType(valueType.getType());
                }
                return new GatewayMetadataMapping(metadataMapping);
            }).map(MetadataMapping.class::cast)
                .collect(Collectors.toMap(MetadataMapping::getId, Function.identity(), (a, b) -> a))
            )
            .orElse(Collections.emptyMap());

        Map<String, MetadataMapping> eventMap = Optional.ofNullable(another.getEvents())
            .map(Collection::stream)
            .map(stream -> stream.collect(Collectors.toMap(MetadataMapping::getId, Function.identity()))).orElse(Collections.emptyMap());
        events = Optional.ofNullable(gatewayDeviceMetadata.getEvents())
            .map(Collection::stream)
            .map(stream -> stream.map(eventMetadata -> {
                    DataType valueType = eventMetadata.getType();
                    MetadataMapping metadataMapping = eventMap.get(eventMetadata.getId());
                    if (metadataMapping==null || !valueType.getType().equals(metadataMapping.getType()) && valueType instanceof ObjectType) {
                        return new GatewayMetadataMapping(eventMetadata, another.getType());
                    }

                    if (!eventMetadata.getName().equals(metadataMapping.getName())) {
                        metadataMapping.setName(eventMetadata.getName());
                    }
                    if (!valueType.getType().equals(metadataMapping.getType())) {
                        metadataMapping.setType(valueType.getType());
                    }
                    return new GatewayMetadataMapping(metadataMapping);
                }).map(MetadataMapping.class::cast)
                    .collect(Collectors.toMap(MetadataMapping::getId, Function.identity(), (a, b) -> a))
            )
            .orElse(Collections.emptyMap());
    }


    public GatewayDeviceMetadataMapping(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }


    @Override
    public List<MetadataMapping> getProperties() {
        if (properties == null && jsonObject != null) {
            properties = Optional.ofNullable(jsonObject.getJSONObject("metadataMapping"))
                .map(json -> json.getJSONArray("properties"))
                .map(Collection::stream)
                .map(stream -> stream
                    .map(JSONObject.class::cast)
                    .map(GatewayMetadataMapping::new)
                    .map(MetadataMapping.class::cast)
                    .collect(Collectors.toMap(MetadataMapping::getId, Function.identity(), (a, b) -> a))
                )
                .orElse(Collections.emptyMap());
        }
        if (properties == null) {
            this.properties = new HashMap<>();
        }
        return new ArrayList<>(properties.values());
    }

    @Override
    public List<MetadataMapping> getEvents() {
        if (events == null && jsonObject != null) {
            events = Optional.ofNullable(jsonObject.getJSONObject("metadataMapping"))
                .map(json -> json.getJSONArray("events"))
                .map(Collection::stream)
                .map(stream -> stream
                    .map(JSONObject.class::cast)
                    .map(GatewayMetadataMapping::new)
                    .map(MetadataMapping.class::cast)
                    .collect(Collectors.toMap(MetadataMapping::getId, Function.identity(), (a, b) -> a))
                )
                .orElse(Collections.emptyMap());
        }
        if (events == null) {
            this.events = new HashMap<>();
        }
        return new ArrayList<>(events.values());
    }

    @Override
    public MetadataMapping getEventOrNull(String id) {
        if (events == null) {
            getEvents();
        }
        return events.get(id);
    }

    @Override
    public MetadataMapping getPropertyOrNull(String id) {
        if (properties == null) {
            getProperties();
        }
        return properties.get(id);
    }

    @Override
    public Map<String, Object> getMsgInfo() {
        if (jsonObject.getJSONObject("msgInfo")==null) return new HashMap<>();
        return jsonObject.getJSONObject("msgInfo");
    }

    @Override
    public Map<String, Object> getMsgType() {
        if (jsonObject.getJSONObject("msgType")==null) return new HashMap<>();
        return jsonObject.getJSONObject("msgType");
    }

    @Override
    public String getType() {
        return jsonObject.getString("type");
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("type", type);
        json.put("properties", getProperties().stream().map(Jsonable::toJson).collect(Collectors.toList()));
        json.put("events", getEvents().stream().map(Jsonable::toJson).collect(Collectors.toList()));
        return json;
    }

    @Override
    public void fromJson(JSONObject json) {
        this.jsonObject = json;
        this.properties = null;
        this.events = null;
        this.type = json.getString("type");
    }

}
