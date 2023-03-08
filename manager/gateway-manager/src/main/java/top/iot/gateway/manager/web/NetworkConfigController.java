package top.iot.gateway.manager.web;

import com.alibaba.fastjson.JSON;
import top.iot.gateway.component.common.utils.ErrorUtils;
import top.iot.gateway.manager.api.response.NetworkConfigInfo;
import top.iot.gateway.manager.api.response.NetworkTypeInfo;
import top.iot.gateway.manager.entity.NetworkConfigEntity;
import top.iot.gateway.manager.enums.NetworkConfigState;
import top.iot.gateway.manager.service.NetworkConfigService;
import top.iot.gateway.network.DefaultNetworkType;
import top.iot.gateway.network.NetworkManager;
import top.iot.gateway.network.NetworkProvider;
import top.iot.gateway.network.mq.devtype.IotDevTypeProducer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.ezorm.rdb.operator.dml.query.SortOrder;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.NotFoundException;
import top.iot.gateway.core.message.property.ReportPropertyMessage;
import top.iot.gateway.supports.cluster.redis.RedisClusterManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author hanyl
 * @since 1.0
 **/
@RestController
@RequestMapping("/network/config")
@Tag(name = "网络组件管理", description = "必须通过指定服务器ID调用")
public class NetworkConfigController implements ReactiveServiceCrudController<NetworkConfigEntity, String> {

    private final NetworkConfigService configService;

    private final NetworkManager networkManager;

    @Autowired
    private RedisClusterManager redisClusterManager;

    public NetworkConfigController(NetworkConfigService configService, NetworkManager networkManager) {
        this.configService = configService;
        this.networkManager = networkManager;
    }

    @Override
    public NetworkConfigService getService() {
        return configService;
    }

    @Resource
    private IotDevTypeProducer iotDevTypeProducer;

    @GetMapping({"/{id}"})
    @QueryAction
    @Operation(
            summary = "根据ID查询网络组件"
    )
    public Mono<NetworkConfigEntity> getById(@PathVariable String id) {
        return this.getService().findById(id).switchIfEmpty(Mono.empty());
    }

    @PostMapping("/save")
    @SaveAction
    @Operation(
            summary = "保存网络组件数据",
            description = "如果传入了id,并且对应数据存在,则尝试覆盖,不存在则新增."
    )
    public Mono<SaveResult> saveNetworkConfig(@RequestBody Mono<NetworkConfigEntity> payload) {
        Mono<NetworkConfigEntity> payload1 = payload
                .switchIfEmpty(ErrorUtils.notFound("网络组件数据不存在"))
                .map(p -> {
                    p.setUpdateTime(new Date());
                    return p;
                });
        return save(Flux.from(payload1));
    }

    @GetMapping("/{networkType}/_detail")
    @QueryAction
    @Operation(summary = "获取指定类型下所有网络组件信息")
    public Flux<NetworkConfigInfo> getNetworkInfo(@PathVariable
                                                  @Parameter(description = "网络组件类型") String networkType) {


        return configService
                .createQuery()
                .where(NetworkConfigEntity::getType, networkType)
                .orderBy(SortOrder.desc(NetworkConfigEntity::getId))
                .fetch()
                .flatMap(config -> {
                    Mono<NetworkConfigInfo> def = Mono.just(NetworkConfigInfo.of(config.getId(), config.getName(), ""));
                    if (config.getState() == NetworkConfigState.enabled) {
                        return networkManager.getNetwork(DefaultNetworkType.valueOf(networkType.toUpperCase()), config.getId())
                                .map(server -> NetworkConfigInfo.of(config.getId(), config.getName(), ""))
                                .onErrorResume(err -> def)
                                .switchIfEmpty(def);
                    }
                    return def;
                });
    }

    @GetMapping("/{networkType}/_detail_state/{serverId}")
    @QueryAction
    @Operation(summary = "获取指定类型和集群的可用网络组件信息")
    public Flux<NetworkConfigInfo> getNetworkInfoState(@PathVariable
                                                       @Parameter(description = "网络组件类型") String networkType,
                                                       @PathVariable
                                                       @Parameter(description = "服务器ID") String serverId) {
        if (!StringUtils.equals(redisClusterManager.getCurrentServerId(), serverId)) {
            return Flux.error(() -> new BusinessException("请选择正确的服务:" + redisClusterManager.getCurrentServerId()));
        }

        return configService
                .createQuery()
                .where(NetworkConfigEntity::getType, networkType)
                .and(NetworkConfigEntity::getServerId, serverId)
                .and(NetworkConfigEntity::getState, NetworkConfigState.enabled)
                .orderBy(SortOrder.desc(NetworkConfigEntity::getId))
                .fetch()
                .flatMap(config -> {
                    Mono<NetworkConfigInfo> def = Mono.just(NetworkConfigInfo.of(config.getId(), config.getName(), ""));
                    if (config.getState() == NetworkConfigState.enabled) {
                        return networkManager.getNetwork(DefaultNetworkType.valueOf(networkType.toUpperCase()), config.getId())
                                .map(server -> NetworkConfigInfo.of(config.getId(), config.getName(), ""))
                                .onErrorResume(err -> def)
                                .switchIfEmpty(def);
                    }
                    return def;
                });
    }

    @GetMapping("/{networkType}/{state}/_detail_state/{serverId}")
    @QueryAction
    @Operation(summary = "获取指定类型和集群并指定状态的网络组件信息")
    public Flux<NetworkConfigInfo> getNetworkInfoState(@PathVariable
                                                       @Parameter(description = "网络组件类型") String networkType,
                                                       @PathVariable(required = false)
                                                       @Parameter(description = "enabled-已启动,disabled-已停止,null-查所有") String state,
                                                       @PathVariable
                                                       @Parameter(description = "服务器ID") String serverId) {
        if (!StringUtils.equals(redisClusterManager.getCurrentServerId(), serverId)) {
            return Flux.error(() -> new BusinessException("请选择正确的服务:" + redisClusterManager.getCurrentServerId()));
        }

        Flux<NetworkConfigEntity> entityFlux;
        NetworkConfigState networkConfigState = NetworkConfigState.toEnum(state);
        if (networkConfigState == null) {
            entityFlux = configService
                    .createQuery()
                    .where(NetworkConfigEntity::getType, networkType)
                    .and(NetworkConfigEntity::getServerId, serverId)
                    .orderBy(SortOrder.desc(NetworkConfigEntity::getId))
                    .fetch();
        } else {
            entityFlux = configService
                    .createQuery()
                    .where(NetworkConfigEntity::getType, networkType)
                    .and(NetworkConfigEntity::getServerId, serverId)
                    .and(NetworkConfigEntity::getState, networkConfigState)
                    .orderBy(SortOrder.desc(NetworkConfigEntity::getId))
                    .fetch();
        }


        return entityFlux
                .flatMap(config -> {
                    Mono<NetworkConfigInfo> def = Mono.just(NetworkConfigInfo.of(config.getId(), config.getName(), ""));
                    if (config.getState() == NetworkConfigState.enabled) {
                        return networkManager.getNetwork(DefaultNetworkType.valueOf(networkType.toUpperCase()), config.getId())
                                .map(server -> NetworkConfigInfo.of(config.getId(), config.getName(), ""))
                                .onErrorResume(err -> def)
                                .switchIfEmpty(def);
                    }
                    return def;
                });
    }

    @GetMapping("/supports")
    @Operation(summary = "获取支持的网络组件类型")
    public Flux<NetworkTypeInfo> getSupports() {
        return Flux.fromIterable(networkManager
                .getProviders())
                .map(NetworkProvider::getType)
                .map(t -> NetworkTypeInfo.of(t.getId(), t.getName()));
    }

    @PostMapping("/{id}/_start")
    @SaveAction
    @Operation(summary = "启动网络组件")
    public Mono<Void> start(@PathVariable
                            @Parameter(description = "网络组件ID") String id) {
        return findNetworkConfig(id)
                .flatMap(conf -> configService.createUpdate()
                        .set(NetworkConfigEntity::getState, NetworkConfigState.enabled)
                        .where(conf::getId)
                        .execute()
                        .thenReturn(conf))
                .flatMap(conf -> networkManager.reload(conf.lookupNetworkType(), id));
    }

    @PostMapping("/{id}/_shutdown")
    @SaveAction
    @Operation(summary = "停止网络组件")
    public Mono<Void> shutdown(@PathVariable
                               @Parameter(description = "网络组件ID") String id) {
        return findNetworkConfig(id)
                .flatMap(conf -> configService.createUpdate()
                        .set(NetworkConfigEntity::getState, NetworkConfigState.disabled)
                        .where(conf::getId)
                        .execute()
                        .thenReturn(conf))
                .flatMap(conf -> networkManager.shutdown(conf.lookupNetworkType(), id));
    }

    private Mono<NetworkConfigEntity> findNetworkConfig(String id) {
        return configService.findById(id)
                .switchIfEmpty(Mono.error(() -> new NotFoundException("配置[" + id + "]不存在")))
                .filter(conf -> StringUtils.equals(redisClusterManager.getCurrentServerId(), conf.getServerId()))
                .switchIfEmpty(Mono.error(() -> new BusinessException("请选择正确的服务:" + redisClusterManager.getCurrentServerId())));
    }

    @GetMapping("/send")
    @Operation(summary = "发送")
    public Mono<Boolean> send() {
        ReportPropertyMessage reportPropertyMessage = new ReportPropertyMessage();
        reportPropertyMessage.setDeviceId("1562007106704510976");
        reportPropertyMessage.setTimestamp(1611132158191L);
        reportPropertyMessage.setProperties(JSON.parseObject("{\n" +
                "        \"atm\": 5.0,\n" +
                "        \"datetime\": \"2021-01-20 04:42:20\",\n" +
                "        \"hum\": 88.0,\n" +
                "        \"noise\": 50.3,\n" +
                "        \"pm10\": 15.0,\n" +
                "        \"pm25\": 12.0,\n" +
                "        \"tem\": -4.9,\n" +
                "        \"tsp\": 0.0,\n" +
                "        \"wd\": 0,\n" +
                "        \"wp\": 0.0,\n" +
                "        \"ws\": 2.0\n" +
                "    }", HashedMap.class));
        return iotDevTypeProducer.sendMessage(reportPropertyMessage);
    }
}
