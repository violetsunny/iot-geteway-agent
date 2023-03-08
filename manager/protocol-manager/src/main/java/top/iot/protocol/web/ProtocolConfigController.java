package top.iot.protocol.web;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import top.iot.protocol.entity.DeviceProtocolConfigEntity;
import top.iot.protocol.godson.metadataMapping.GatewayMetadataMapping;
import top.iot.protocol.service.LocalProtocolConfigService;
import top.iot.protocol.web.request.ProtocolConfigDecodeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import top.iot.gateway.core.ProtocolSupport;
import top.iot.gateway.core.ProtocolSupports;
import top.iot.gateway.core.message.codec.Transport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/protocol/config")
@Authorize(ignore = true)
@Resource(id = "protocol-config", name = "协议配置管理")
@Tag(name = "协议配置管理")
@EnableEasyormRepository("top.iot.protocol.web.response")
public class ProtocolConfigController implements ReactiveServiceCrudController<DeviceProtocolConfigEntity, String> {
    @Autowired
    @Getter
    private LocalProtocolConfigService service;

//    private ReactiveRepository<SupportedProtocolTransport,Integer> protocolTransportRepository;

    @Value("${top.iot.product-api.query-by-id-url}")
    private String queryMeasurePropertiesUrl;
    @Value("${top.iot.device.url}")
    private String deviceApiUrl;
    @Value("${top.iot.product-api.measure-properties-jsonpath}")
    private String measurePropertiesJsonPath;

    @PostMapping("/save")
    @SaveAction
    @Operation(summary = "新增或修改单个数据,并返回新增或修改后的数据.")
    //增加保存时间戳
    public Mono<DeviceProtocolConfigEntity> save(@RequestBody DeviceProtocolConfigEntity payload) {
        return Authentication.currentReactive()
                .flatMap(auth -> Mono.just(applyAuthentication(payload, auth)))
                .switchIfEmpty(Mono.just(payload))
                .map(p -> {
                    String meta = p.getMetadataMapping();
                    p.setMetadataMapping(addTimestampToMetadataMapping(meta));
                    return p;
                })
                .flatMap(service::_save);
    }

    @GetMapping("/_queryId")
    @QueryAction
    @Operation(summary = "根据id查询协议配置，没有协议配置时，id转为产品Id查询物模型")
    //查询结果默认增加时间戳
    public Mono<PagerResult<DeviceProtocolConfigEntity>> queryPager(@Parameter String queryId) {
        QueryParamEntity query = QueryParamEntity.of();
        query.setTerms(new Term().and("id", queryId).getTerms());
        return this.queryPager(Mono.just(query))
                .flatMap(p -> {
                    if (p.getData() == null || p.getData().isEmpty()) {//配置协议未找到则调用物模型接口
                        return queryInProductMeta(queryId)
                                .map(pc -> {
                                    List<DeviceProtocolConfigEntity> data = new ArrayList<>();
                                    if (pc.getMetadataMapping() != null) {
                                        data.add(pc);
                                        return PagerResult.of(1, data);
                                    }
                                    return PagerResult.of(0, data);
                                });
                    }
                    return Mono.just(p);
                })
                .doOnNext(p -> {//统一添加时间戳
                    List<DeviceProtocolConfigEntity> data = p.getData();
                    if (!data.isEmpty()) {
                        String meta = data.get(0).getMetadataMapping();
                        data.get(0).setMetadataMapping(addTimestampToMetadataMapping(meta));
                    }
                });
    }

    private Mono<DeviceProtocolConfigEntity> queryInProductMeta(String productId) {
        try {//通过物模型接口，基于产品code查询物模型测量属性
            URL url = new URL(deviceApiUrl + queryMeasurePropertiesUrl + productId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.connect();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            JSONObject jsonObj = JSON.parseObject(reader.readLine());
            DeviceProtocolConfigEntity pc = new DeviceProtocolConfigEntity();
            if (jsonObj != null && jsonObj.getIntValue("code") == 200) {
                pc.setId(productId);
                JSONArray measureProperties = (JSONArray) JSONPath.eval(jsonObj, measurePropertiesJsonPath);
                if (measureProperties == null) return Mono.just(pc);
                List<Object> properties = new ArrayList<>();
                for (int i = 0; i < measureProperties.size(); i++) {
                    JSONObject measure = measureProperties.getJSONObject(i);
                    String propertyName = measure.get("name").toString();
                    String id = measure.get("code").toString();
                    String type = measure.get("type").toString();
                    JSONObject property = buildPropertyJson(propertyName, id, type);
                    properties.add(property);
                }
                JSONObject metaMapping = buildMetadataMappingJson(properties, new ArrayList<>());
                pc.setMetadataMapping(metaMapping.toString());
            }
            reader.close();
            connection.disconnect();
            return Mono.just(pc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String addTimestampToMetadataMapping(String metadataMapping) {
        JSONObject jsonObj = JSON.parseObject(metadataMapping);
        JSONArray timestampProperty = (JSONArray) JSONPath.eval(jsonObj, "$.properties[id='timestamp']");
        if (jsonObj == null || timestampProperty == null) {//未包含properties
            JSONObject timestamp = buildPropertyJson("时间戳", "timestamp", "long");
            List<Object> properties = new ArrayList<>();
            properties.add(timestamp);
            metadataMapping = buildMetadataMappingJson(properties, new ArrayList<>()).toString();
        } else if (timestampProperty.isEmpty()) {//properties未包含时间戳属性
            JSONArray properties = jsonObj.getJSONArray("properties");
            properties.add(buildPropertyJson("时间戳", "timestamp", "long"));
            jsonObj.put("properties", properties);
            metadataMapping = jsonObj.toString();
        }
        return metadataMapping;
    }

    private JSONObject buildPropertyJson(String propertyName, String id, String type) {
        GatewayMetadataMapping property = new GatewayMetadataMapping();
        property.setId(id);
        property.setName(propertyName);
        property.setComplex(false);
        property.setType(type);
        Map<String, Object> expression = new HashMap<>();
        expression.put("lang", "javascript");
        expression.put("script", "");
        List<String> reports = new ArrayList<>();
        reports.add("$.properties." + id);
        property.setReports(reports);
        property.setExpression(expression);
        return property.toJson();
    }

    private JSONObject buildMetadataMappingJson(List<Object> properties, List<Object> events) {
        JSONObject json = new JSONObject();
        json.put("properties", properties);
        json.put("events", events);
        return json;
    }

    @Autowired
    private ProtocolSupports protocolSupports;
    @GetMapping("/_queryProtocol")
    @QueryAction
    @Operation(summary = "获取支持的消息协议下拉列表")
    //包括内置的协议：标准mqtt、godson；已发布的配置协议
    public Flux<String> querySupportedProtocol() {
        return protocolSupports.getProtocols().filter(p-> !p.getId().equals("iot.gateway.v1.0"))
                .mapNotNull(ProtocolSupport::getId);
    }
    @GetMapping(value = {"/_queryTransport/{protocol}"})
    @QueryAction
    @Operation(summary = "获取消息协议支持的传输协议下拉列表")
    public Flux<String> querySupportedTransport(@PathVariable String protocol){
        return protocolSupports.getProtocols()
                .filter(p-> !p.getId().equals("iot.gateway.v1.0"))
                .filter(p -> p.getId().equals(protocol))
                .flatMap(ProtocolSupport::getSupportedTransport)
                .map(Transport::getName);
    }

    @PostMapping("/deploy")
    @Authorize(ignore = true)
    @SaveAction
    public Mono<DeviceProtocolConfigEntity> deploy(@RequestBody DeviceProtocolConfigEntity payload) {
        return service.deploy(payload);
    }

    @PostMapping("/decode")
    public Mono<String> decode(@RequestBody Mono<ProtocolConfigDecodeRequest> entity) {
        return service.decode(entity);
    }

    /**
     * 参数调试
     *
     * @param entity entity中的payload：如果是json, 传递整个json消息;如果非json, 传递单个值
     * @return
     */
    @PostMapping("/parameter/decode")
    public Mono<String> parameterDecode(@RequestBody Mono<ProtocolConfigDecodeRequest> entity) {
        return service.parameterDecode(entity);
    }

}
