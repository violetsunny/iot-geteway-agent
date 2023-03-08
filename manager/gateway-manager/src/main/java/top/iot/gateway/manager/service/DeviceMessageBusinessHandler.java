package top.iot.gateway.manager.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.iot.gateway.core.event.EventBus;
import top.iot.gateway.core.event.Subscription;
import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.utils.FluxUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class DeviceMessageBusinessHandler {


    private final EventBus eventBus;

    private final LocalDeviceInstanceService deviceService;

    @PostConstruct
    public void init() {

        Subscription subscription = Subscription
                .builder()
                .subscriberId("device-state-synchronizer")
                .topics("/device/*/*/online", "/device/*/*/offline")
                .justLocal()//只订阅本地
                .build();

        //订阅设备上下线消息,同步数据库中的设备状态,
        //如果2条消息间隔大于0.8秒则不缓冲直接更新
        //否则缓冲,数量超过500后批量更新
        //无论缓冲区是否超过500条,都每2秒更新一次.
        FluxUtils.bufferRate(eventBus
                        .subscribe(subscription, DeviceMessage.class)
                        .map(DeviceMessage::getDeviceId),
                800, Integer.getInteger("device.state.sync.batch", 500), Duration.ofSeconds(2))
                .onBackpressureBuffer(64,
                        list -> log.warn("无法处理更多设备状态同步!"),
                        BufferOverflowStrategy.DROP_OLDEST)
                .publishOn(Schedulers.boundedElastic(), 64)
                .concatMap(list -> deviceService.syncStateBatch(Flux.just(list), false).map(List::size))
                .onErrorContinue((err, obj) -> log.error(err.getMessage(), err))
                .subscribe((i) -> log.info("同步设备状态成功:{}", i));

    }


}
