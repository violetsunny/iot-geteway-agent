package top.iot.gateway.manager.web;

import top.iot.gateway.component.common.utils.ErrorUtils;
import top.iot.gateway.component.common.utils.RegexUtil;
import top.iot.gateway.component.gateway.DeviceGateway;
import top.iot.gateway.component.gateway.DeviceGatewayManager;
import top.iot.gateway.component.gateway.supports.DeviceGatewayProvider;
import top.iot.gateway.manager.api.response.DeviceGatewayQueryRes;
import top.iot.gateway.manager.entity.DeviceGatewayEntity;
import top.iot.gateway.manager.enums.NetworkConfigState;
import top.iot.gateway.manager.service.DeviceGatewayService;
import top.iot.gateway.manager.service.NetworkConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang.StringUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.BusinessException;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.supports.cluster.redis.RedisClusterManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.*;

@RestController
@RequestMapping("gateway/device")
@Tag(name = "接入网关管理", description = "必须通过指定服务器ID调用")
public class DeviceGatewayController implements ReactiveServiceCrudController<DeviceGatewayEntity, String> {

    private final DeviceGatewayService deviceGatewayService;

    @Resource
    private NetworkConfigService configService;

    @Autowired
    private RedisClusterManager redisClusterManager;

    @Override
    public DeviceGatewayService getService() {
        return deviceGatewayService;
    }

    private final DeviceGatewayManager gatewayManager;

    public DeviceGatewayController(DeviceGatewayManager gatewayManager, DeviceGatewayService deviceGatewayService) {
        this.gatewayManager = gatewayManager;
        this.deviceGatewayService = deviceGatewayService;
    }

    @PostMapping("/save")
    @SaveAction
    @Operation(
            summary = "保存接入网关数据",
            description = "如果传入了id,并且对应数据存在,则尝试覆盖,不存在则新增."
    )
    public Mono<SaveResult> saveGateway(@RequestBody Mono<DeviceGatewayEntity> payload) {
        //对于http协议下的路由url规则，只能英文、数字、字符~!@#$%^&*()-_+<>?
        Mono<DeviceGatewayEntity> payload1 = payload.switchIfEmpty(ErrorUtils.notFound("网关数据不存在"))
                .filter(p -> {
                    if (p.getProvider().equals("http-server-gateway")) {
                        ArrayList<Map<String, String>> routes = (ArrayList<Map<String, String>>) p.getConfiguration().get("routes");
                        Boolean flag = true;
                        for (Map<String, String> route : routes) {
                            String url = route.get("url");
                            flag = RegexUtil.validateId(url);
                            if (!flag) break;
                        }
                        return flag;
                    }
                    return true;
                })
                .switchIfEmpty(Mono.error(() -> new BusinessException("协议路由输入错误")))
                .map(p -> {
                    p.setUpdateTime(new Date());
                    return p;
                });
        return save(Flux.from(payload1));
    }

    @PostMapping({"/_queryPage"})
    @QueryAction
    @Operation(
            summary = "使用POST方式分页动态查询网关列表"
    )
    public Mono<PagerResult<DeviceGatewayQueryRes>> queryPagerGateway(@RequestBody Mono<QueryParamEntity> query) {
        //返回分页查询结果，在原实体类基础上，增加网关类型provider.name，网络组件network.name
        Map<String, String> providersMap = new HashMap<>();
        gatewayManager.getProviders().forEach(provider->{
            providersMap.put(provider.getId(), provider.getName());
        });
        List<DeviceGatewayQueryRes> out = new ArrayList<>();
        PagerResult<DeviceGatewayQueryRes> result = new PagerResult();
        return this.getService().queryPager(query).map(r -> {
                    for (DeviceGatewayEntity data : r.getData()) {
                        String id = data.getId() != null ? data.getId() : "";
                        String name = data.getName() != null ? data.getName() : "";
                        String serverId = data.getServerId() != null ? data.getServerId() : "";
                        String provider = data.getProvider();
                        String providerName = providersMap.getOrDefault(provider, "");
                        String networkId = data.getNetworkId() != null ? data.getNetworkId() : "";
                        String networkName = "";
                        Map<String, String> state = new HashMap<>();
                        String stateText = data.getState() != null ? data.getState().getText() : "";
                        state.put("text", stateText);
                        String stateValue = data.getState() != null ? data.getState().getValue() : "";
                        state.put("value", stateValue);
                        Map<String, Object> configuration = data.getConfiguration();
                        String describe = data.getDescribe() != null ? data.getDescribe() : "";
                        Date createTime = data.getCreateTime();
                        Date updateTime = data.getUpdateTime();
                        DeviceGatewayQueryRes dg = DeviceGatewayQueryRes.of
                                (id, name, serverId, provider, providerName, networkId, networkName,
                                        state, configuration, describe, createTime, updateTime);
                        out.add(dg);
                    }
                    result.setData(out);
                    result.setTotal(r.getTotal());
                    result.setPageIndex(r.getPageIndex());
                    result.setPageSize(r.getPageSize());
                    return out;
                })
                .flatMapMany(Flux::fromIterable)
                .flatMap(d -> {
                    QueryParamEntity netQuery = QueryParamEntity.of();
                    netQuery.setTerms(new Term().and("id", d.getNetworkId()).getTerms());
                    return configService.queryPager(netQuery).doOnNext(r -> {
                        if (!r.getData().isEmpty()) d.setNetworkName(r.getData().get(0).getName());
                    });
                })
                .then(Mono.just(result));
    }

    @PostMapping("/{id}/_startup")
    @SaveAction
    @Operation(summary = "启动网关")
    public Mono<Void> startup(@PathVariable
                              @Parameter(description = "网关ID") String id) {

        return findDeviceGateway(id)
                .then(gatewayManager.getGateway(id).flatMap(DeviceGateway::startup))
                .then(deviceGatewayService.updateState(id, NetworkConfigState.enabled))
                .then();

    }

    @PostMapping("/{id}/_pause")
    @SaveAction
    @Operation(summary = "暂停")
    public Mono<Void> pause(@PathVariable
                            @Parameter(description = "网关ID") String id) {

        return findDeviceGateway(id)
                .then(gatewayManager.getGateway(id).flatMap(DeviceGateway::pause))
                .then(deviceGatewayService.updateState(id, NetworkConfigState.paused))
                .then();
    }

    @PostMapping("/{id}/_shutdown")
    @SaveAction
    @Operation(summary = "停止")
    public Mono<Void> shutdown(@PathVariable
                               @Parameter(description = "网关ID") String id) {
        return findDeviceGateway(id)
                .then(gatewayManager.shutdown(id))
                .then(deviceGatewayService.updateState(id, NetworkConfigState.disabled).then());
    }

    private Mono<Object> findDeviceGateway(String id) {
        return deviceGatewayService.findById(id)
                .filter(gate -> StringUtils.equals(redisClusterManager.getCurrentServerId(), gate.getServerId()))
                .switchIfEmpty(Mono.error(() -> new BusinessException("请选择正确的服务:" + redisClusterManager.getCurrentServerId())))
                .flatMap(gate -> configService.findById(gate.getNetworkId())
                        .switchIfEmpty(Mono.empty())
                        .filter(config -> config.getState() != null && StringUtils.equals(NetworkConfigState.enabled.getValue(), config.getState().getValue()))
                        .switchIfEmpty(Mono.error(() -> new BusinessException("网络组件[" + gate.getNetworkId() + "]已被禁用，停止失败"))));
    }

    @GetMapping(value = "/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @QueryAction
    @Operation(summary = "从设备网关中订阅消息")
    public Flux<Message> getMessages(@PathVariable
                                     @Parameter(description = "网关ID") String id) {
        return gatewayManager
                .getGateway(id)
                .flatMapMany(DeviceGateway::onMessage);
    }

    @GetMapping(value = "/providers")
    @Operation(summary = "获取支持的设备网关")
    public Flux<DeviceGatewayProvider> getProviders() {
        return Flux.fromIterable(gatewayManager.getProviders());
    }


}
