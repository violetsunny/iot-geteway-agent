package top.iot.gateway.agent.web;

import top.iot.gateway.manager.service.LocalDeviceInstanceService;
import top.iot.gateway.network.DeviceCacheModel;
import top.iot.gateway.network.DeviceCacheOperator;
import com.google.common.collect.Lists;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.ezorm.core.param.QueryParam;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.api.crud.entity.PagerResult;
import top.iot.gateway.core.config.ConfigStorageManager;
import top.iot.gateway.core.device.DeviceConfigKey;
import top.iot.gateway.core.device.DeviceRegistry;
import top.iot.gateway.core.device.session.DeviceSessionManager;
import top.iot.gateway.core.server.session.DeviceSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@RequestMapping("/session")
@RestController
@Tag(name = "会话管理")
@Slf4j
public class SessionController {

    @Autowired
    private DeviceSessionManager deviceSessionManager;
    @Autowired
    private ConfigStorageManager configStorageManager;
    @Autowired
    private LocalDeviceInstanceService localDeviceInstanceService;
    @Autowired
    private DeviceRegistry registry;

    @GetMapping("/all")
    @Operation(summary = "获取会话")
    public Mono<PagerResult<DeviceSession>> getSession(@Parameter(description = "会话请求数据") QueryParam queryParam) {
//        String serverId = "";
//        for (Term term : queryParam.getTerms()) {
//            if ("serverId".equalsIgnoreCase(term.getColumn())) {
//                serverId = (String) term.getValue();
//            }
//        }
//        if (!StringUtils.equals(redisClusterManager.getCurrentServerId(), serverId)) {
//            return Mono.error(() -> new BusinessException("请选择正确的服务:" + redisClusterManager.getCurrentServerId()));
//        }
        return deviceSessionManager.getSessions()
                .filter(session -> {
                    boolean flag = true;
                    for (Term term : queryParam.getTerms()) {
                        if ("transport".equalsIgnoreCase(term.getColumn()) && StringUtils.isNotBlank(String.valueOf(term.getValue()))) {
                            flag = String.valueOf(term.getValue()).contains(session.getTransport().getId());
                        } else if ("deviceId".equalsIgnoreCase(term.getColumn())) {
                            flag = term.getValue().equals(session.getDeviceId());
                        }
                        if (!flag) break;
                    }
                    return flag;
                })
                .sort((session1, session2) -> (int) (session2.connectTime() - session1.connectTime()))
                .collectList()
                .map(list -> {
                    List<DeviceSession> sessions = new ArrayList<>();
                    int pageIndex = queryParam.getPageIndex();
                    int pageSize = queryParam.getPageSize();
                    if (list.size() >= pageIndex * pageSize && list.size() < (pageIndex + 1) * pageSize) {
                        sessions = list.subList(pageIndex * pageSize, list.size());
                    } else if (list.size() >= (pageIndex + 1) * pageSize) {
                        sessions = list.subList(pageIndex * pageSize, (pageIndex + 1) * pageSize);
                    }
                    return PagerResult.of(list.size(), sessions, queryParam);
                }).switchIfEmpty(Mono.just(PagerResult.of(0, Lists.newArrayList())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据id获取会话")
    public Mono<DeviceSession> getSessionById(@PathVariable String id) {
        return deviceSessionManager.getSession(id);
    }

    @PostMapping("/unregister/{id}")
    @Operation(summary = "移除会话-下线")
    public Mono<Long> unregister(@PathVariable String id) {
        //先removeSession中会话，会修改缓存设备状态，然后再调用设备下线
        return deviceSessionManager.remove(id, false)
                .flatMap(res -> localDeviceInstanceService.syncStateBatch(Flux.just(Lists.newArrayList(id)), false).then(Mono.just(res)));
    }

    @PostMapping("/setCache")
    public Flux<Boolean> setCache(@RequestBody DeviceCacheModel cacheModel) {
        String cacheKey;
        if (StringUtils.isBlank(cacheModel.getIndex())) {
            cacheKey = "device:" + cacheModel.getDeviceId();
        } else {
            cacheKey = cacheModel.getIndex();
        }
        DeviceCacheOperator operator = new DeviceCacheOperator(cacheKey, configStorageManager);
        return Flux.fromIterable(cacheModel.getValues().entrySet())
                .flatMap(entry -> operator.setConfig(entry.getKey(), entry.getValue()));
    }

    @PostMapping("/getCache")
    public Mono getCache(@RequestBody DeviceCacheModel cacheModel) {
        String cacheKey;
        if (StringUtils.isBlank(cacheModel.getIndex())) {
            cacheKey = "device:" + cacheModel.getDeviceId();
        } else {
            cacheKey = cacheModel.getIndex();
        }
        DeviceCacheOperator operator = new DeviceCacheOperator(cacheKey, configStorageManager);
        if (CollectionUtils.isNotEmpty(cacheModel.getKeys())) {
            return operator.getConfigs(cacheModel.getKeys());
        }
        return operator.getConfig(DeviceConfigKey.productId);
    }
}
