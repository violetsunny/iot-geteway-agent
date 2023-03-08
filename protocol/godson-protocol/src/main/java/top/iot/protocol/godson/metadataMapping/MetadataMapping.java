package top.iot.protocol.godson.metadataMapping;

import top.iot.gateway.core.metadata.Jsonable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface MetadataMapping extends Jsonable {
    String getId();

    String getName();

    List<String> getReports();

    Map<String, Object> getExpression();

    boolean isComplex();

    String getType();

    default Optional<Object> getExpression(String name) {
        return Optional.ofNullable(this.getExpression()).map((map) -> {
            return map.get(name);
        });
    }

    List<? extends MetadataMapping> getParameters();

    Map<String, Object> getExpands();

    default Optional<Object> getExpand(String name) {
        return Optional.ofNullable(this.getExpands()).map((map) -> {
            return map.get(name);
        });
    }

    void setName(String name);

    void setType(String type);
}
