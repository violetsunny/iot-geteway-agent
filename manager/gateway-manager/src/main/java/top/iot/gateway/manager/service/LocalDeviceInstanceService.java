package top.iot.gateway.manager.service;

import top.iot.gateway.component.common.utils.ErrorUtils;
import top.iot.gateway.manager.entity.DeviceStateInfo;
import top.iot.gateway.manager.enums.DeviceState;
import top.iot.gateway.network.integration.DeviceClient;
import top.iot.gateway.network.integration.model.DeviceStateReq;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.id.IDGenerator;
import top.iot.gateway.core.device.DeviceConfigKey;
import top.iot.gateway.core.device.DeviceRegistry;
import top.iot.gateway.core.enums.ErrorCode;
import top.iot.gateway.core.exception.DeviceOperationException;
import top.iot.gateway.core.message.DeviceMessageReply;
import top.iot.gateway.core.message.FunctionInvokeMessageSender;
import top.iot.gateway.core.message.function.FunctionInvokeMessageReply;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocalDeviceInstanceService {

    @Resource
    private DeviceRegistry registry;

    @Resource
    private DeviceClient deviceClient;


    public Flux<List<DeviceStateInfo>> syncStateBatch(Flux<List<String>> batch, boolean force) {

        return batch
                .concatMap(list -> Flux
                        .fromIterable(list)
                        .publishOn(Schedulers.parallel())
                        .flatMap(id -> registry
                                .getDevice(id)
                                .flatMap(operator -> {
                                    Mono<Byte> state = force ? operator.checkState() : operator.getState();
                                    return Mono.zip(
                                            state.defaultIfEmpty(top.iot.gateway.core.device.DeviceState.offline),//状态
                                            Mono.just(operator.getDeviceId()), //设备id
                                            operator.getConfig(DeviceConfigKey.isGatewayDevice)
                                                    .defaultIfEmpty(false)//是否为网关设备
                                    );
                                })
                                //注册中心里不存在设备就认为是未激活.   t1=状态，t2=deviceId，t3=是否网关设备
                                .defaultIfEmpty(Tuples.of(top.iot.gateway.core.device.DeviceState.noActive, id, false)))
                        .collect(Collectors.groupingBy(Tuple3::getT1))
                        .flatMapIterable(Map::entrySet)
                        .flatMap(group -> {
                            List<String> deviceIdList = group
                                    .getValue()
                                    .stream()
                                    .map(Tuple3::getT2)
                                    .collect(Collectors.toList());
                            DeviceState state = DeviceState.of(group.getKey());

                            //批量修改设备状态、批量修改子设备状态
                            return Flux
                                    .concat(
                                            //批量修改设备状态
//                                            this.getRepository()
//                                                    .createUpdate()
//                                                    .set(DeviceInstanceEntity::getState, state)
//                                                    .where()
//                                                    .in(DeviceInstanceEntity::getId, deviceIdList)
//                                                    .execute()
//                                                    .thenReturn(group.getValue().size())

                                            Mono.just(DeviceStateReq.of(deviceIdList, DeviceState.of(state), "1"))
                                                    .map(req -> deviceClient.batchUpdateDeviceState(req))
                                            ,
                                            //修改子设备状态
                                            Flux.fromIterable(group.getValue())
                                                    .filter(Tuple3::getT3) //过滤是网关设备的Id
                                                    .map(Tuple3::getT2)
                                                    .collectList()
                                                    .filter(CollectionUtils::isNotEmpty)
                                                    .flatMap(parents ->
//                                                            this
//                                                            .getRepository()
//                                                            .createUpdate()
//                                                            .set(DeviceInstanceEntity::getState, state)
//                                                            .where()
//                                                            .in(DeviceInstanceEntity::getParentId, parents)
//                                                            //不修改未激活的状态
//                                                            .not(DeviceInstanceEntity::getState, DeviceState.notActive)
//                                                            .nest()
//                                                            /* */.accept(DeviceInstanceEntity::getFeatures, Terms.Enums.notInAny, DeviceFeature.selfManageState)
//                                                            /* */.or()
//                                                            /* */.isNull(DeviceInstanceEntity::getFeatures)
//                                                            .end()
//                                                            .execute())
//                                                            .defaultIfEmpty(0)

                                                                    //如果是子设备则DeviceIds是通过ParentId更新子设备
                                                                    Mono.just(DeviceStateReq.of(parents, DeviceState.of(state), "2"))
                                                                            .map(req -> deviceClient.batchUpdateDeviceState(req))
                                                    ))
                                    .then(Mono.just(
                                            deviceIdList
                                                    .stream()
                                                    .map(id -> DeviceStateInfo.of(id, state))
                                                    .collect(Collectors.toList())
                                    ));

                        }));
    }

    //设备功能调用
    @SneakyThrows
    public Flux<?> invokeFunction(String deviceId,
                                  String functionId,
                                  Map<String, Object> properties) {
        return registry
                .getDevice(deviceId)
                .switchIfEmpty(ErrorUtils.notFound("设备不存在"))
                .flatMap(operator -> operator
                        .messageSender()
                        .invokeFunction(functionId)
                        .messageId(IDGenerator.SNOW_FLAKE_STRING.generate())
                        .setParameter(properties)
                        .validate()
                )
                .flatMapMany(FunctionInvokeMessageSender::send)
                .flatMap(mapReply(FunctionInvokeMessageReply::getOutput));
    }

    private static <R extends DeviceMessageReply, T> Function<R, Mono<T>> mapReply(Function<R, T> function) {
        return reply -> {
            if (ErrorCode.REQUEST_HANDLING.name().equals(reply.getCode())) {
                throw new DeviceOperationException(ErrorCode.REQUEST_HANDLING, reply.getMessage());
            }
            if (!reply.isSuccess()) {
                throw new BusinessException(reply.getMessage(), reply.getCode());
            }
            return Mono.justOrEmpty(function.apply(reply));
        };
    }

}
