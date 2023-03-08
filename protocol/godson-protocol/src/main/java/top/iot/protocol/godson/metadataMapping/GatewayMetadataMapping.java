package top.iot.protocol.godson.metadataMapping;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import top.iot.gateway.core.metadata.EventMetadata;
import top.iot.gateway.core.metadata.PropertyMetadata;
import top.iot.gateway.core.metadata.types.ObjectType;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor
public class GatewayMetadataMapping implements MetadataMapping {

    @Getter
    @Setter
    private boolean complex;
    @Getter
    @Setter
    private String type;

    @Getter
    @Setter
    private List<GatewayMetadataMapping> parameters = new ArrayList<>();

    @Getter
    @Setter
    private String id;
    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private Map<String, Object> expression = new HashMap<>();
    @Getter
    @Setter
    private List<String> reports = new ArrayList<>();
    @Getter
    @Setter
    private Map<String, Object> expands = new HashMap<>();


    public GatewayMetadataMapping(JSONObject json) {
        this.fromJson(json);
    }

    @SuppressWarnings("unchecked")
    public GatewayMetadataMapping(MetadataMapping another) {
        this.id = another.getId();
        this.name = another.getName();
        this.expression = another.getExpression();
        this.reports = another.getReports();
        this.complex = another.isComplex();
        this.type = another.getType();
        this.parameters = (List<GatewayMetadataMapping>)another.getParameters();
        this.expands = another.getExpands();
    }

    GatewayMetadataMapping(PropertyMetadata propertyMetadata, String type, String... prefix) {
        this.id = propertyMetadata.getId();
        this.name = propertyMetadata.getName();
        if (StringUtils.isEmpty(type) || "JSON".equals(type)) {
            if (prefix.length ==0) {
                this.reports.add("$.properties." + id);
            } else {
                this.reports.add(prefix[0] + "." + id);
            }
        }

        String expressionStr = "{\n" +
            "\t\"lang\": \"javascript\",\n" +
            "\t\"script\": \"\"\n" +
            "}";

        this.expression = JSON.parseObject(expressionStr);
        this.type = propertyMetadata.getValueType().getType();
        if (propertyMetadata.getValueType() instanceof ObjectType) {
            complex = true;
            List<PropertyMetadata> properties = ((ObjectType) propertyMetadata.getValueType()).getProperties();
            properties.forEach(meta -> parameters.add(new GatewayMetadataMapping(meta, type, this.reports.size()==0 ? "" : this.reports.get(0))));
        }
    }

    GatewayMetadataMapping(EventMetadata eventMetadata, String type, String... prefix) {
        this.id = eventMetadata.getId();
        this.name = eventMetadata.getName();
        if (StringUtils.isEmpty(type) || "JSON".equals(type)) {
            if (prefix.length ==0) {
                this.reports.add("$.events." + id);
            } else {
                this.reports.add(prefix[0] + "." + id);
            }
        }

        String expressionStr = "{\n" +
            "\t\"lang\": \"javascript\",\n" +
            "\t\"script\": \"\"\n" +
            "}";

        this.expression = JSON.parseObject(expressionStr);
        this.type = eventMetadata.getType().getType();
        if (eventMetadata.getType() instanceof ObjectType) {
            complex = true;
            List<PropertyMetadata> properties = ((ObjectType) eventMetadata.getType()).getProperties();
            properties.forEach(meta -> parameters.add(new GatewayMetadataMapping(meta, type, this.reports.size()==0 ? "" : this.reports.get(0))));
        }
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", this.id);
        json.put("name", this.name);
        json.put("expression", this.expression);
        json.put("reports", this.reports);
        json.put("complex", this.complex);
        json.put("parameters", this.parameters);
        json.put("type", this.type);
        json.put("expands", this.expands);
        return json;
    }

    public void fromJson(JSONObject jsonObject) {
        Objects.requireNonNull(jsonObject);
        if (jsonObject.getJSONArray("reports") != null) {
            this.id = jsonObject.getString("id");
            this.name = jsonObject.getString("name");
            this.expression = jsonObject.getJSONObject("expression");
            this.reports = jsonObject.getJSONArray("reports").toJavaList(String.class);
            this.complex = jsonObject.getBoolean("complex");
            this.type = jsonObject.getString("type");
            if (jsonObject.getJSONArray("parameters") != null) {
                this.parameters = jsonObject.getJSONArray("parameters").stream().map(json -> new GatewayMetadataMapping(JSON.parseObject(json.toString()))).collect(Collectors.toList());
            }
            this.expands = jsonObject.getJSONObject("expands");
        }

    }

    public String toString() {
        return String.join("",  this.getId(), " /* ", this.getName(), " */ ", JSONObject.toJSONString(this.getExpression()), "*/", JSONObject.toJSONString(this.getReports()), "*/", this.complex+"", "*/", this.type, "*/", JSONObject.toJSONString(this.expands));
    }

}

