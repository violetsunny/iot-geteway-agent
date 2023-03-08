/**
 * llkang.com Inc.
 * Copyright (c) 2010-2022 All Rights Reserved.
 */
package top.iot.gateway.network.mq.devtype;

import com.alibaba.fastjson.JSON;
import top.iot.gateway.network.DeviceMessageConfigKey;
import top.iot.gateway.network.integration.DeviceClient;
import top.iot.gateway.network.mq.KafkaProducer;
import lombok.extern.slf4j.Slf4j;
import top.iot.gateway.core.device.DeviceRegistry;
import top.iot.gateway.core.message.ChildDeviceMessage;
import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.property.ReportPropertyMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 物联设备类型消息生产者
 *
 * @author kanglele
 * @version $Id: IotDevTypeProducer, v 0.1 2022/8/22 10:24 kanglele Exp $
 */
@Component
@Slf4j
public class IotDevTypeProducer {

    @Resource
    private KafkaProducer kafkaProducer;
    @Resource
    private DeviceClient deviceClient;
    @Resource
    private DeviceRegistry registry;

    @Value("${top.iot.device.mengniu_topic:data_iot_mengniu_dev}")
    private String deviceTopic;

    public Mono<Boolean> sendMessage(DeviceMessage deviceMessage) {
        if (Objects.isNull(deviceMessage)) {
            return Mono.empty();
        }

        if (deviceMessage instanceof ReportPropertyMessage) {
            return send((ReportPropertyMessage) deviceMessage);
        }

        if (deviceMessage instanceof ChildDeviceMessage) {
            Message childMessage = ((ChildDeviceMessage) deviceMessage).getChildDeviceMessage();
            if (childMessage instanceof ReportPropertyMessage) {
                return send((ReportPropertyMessage) childMessage);
            }
        }

        return Mono.empty();
    }

    private Mono<Boolean> send(ReportPropertyMessage deviceMessage) {
        String devId = deviceMessage.getDeviceId();
        //DeviceDataRes deviceDataRes = deviceClient.getDeviceId(devId);
        //从redis获取
        return registry.getDevice(devId)
                .flatMap(cache -> {

                    IotDevTypeMessage message = new IotDevTypeMessage();
                    message.setVersion("0.0.1");
                    message.setDevId(devId);
                    message.setTs(deviceMessage.getTimestamp());
                    message.setResume(deviceMessage.getHeader("resume").isPresent()? String.valueOf(deviceMessage.getHeader("resume").get()) :  "N");
                    message.setSource("custom");
                    Map<String, Object> properties = deviceMessage.getProperties();
                    if (properties != null && properties.containsKey("timestamp")) {
                        properties.remove("timestamp");
                    }
                    List<IotDevTypeMessage.Metrics> metrics = new ArrayList<>();
                    for (Map.Entry<String, Object> entry : properties.entrySet()) {
                        IotDevTypeMessage.Metrics metric = new IotDevTypeMessage.Metrics();
                        metric.setMetric(entry.getKey());
                        metric.setValue(entry.getValue());
                        metrics.add(metric);
                    }
                    message.setData(metrics);

                    return Mono.just(message)
                            .flatMap(msg ->
                                    cache.getSelfConfigs(DeviceMessageConfigKey.deviceType,
                                            DeviceMessageConfigKey.productId,
                                            DeviceMessageConfigKey.tenantId,
                                            DeviceMessageConfigKey.deviceName,
                                            DeviceMessageConfigKey.period,
                                            DeviceMessageConfigKey.sn,
                                            DeviceMessageConfigKey.testFlag)
                                            .flatMap(values -> {
                                                msg.setDevType(values.getString(DeviceMessageConfigKey.deviceType.getKey(), ""));
                                                msg.setDeviceName(values.getString(DeviceMessageConfigKey.deviceName.getKey(), ""));
                                                msg.setPeriod(values.getString(DeviceMessageConfigKey.period.getKey(), ""));
                                                msg.setSn(values.getString(DeviceMessageConfigKey.sn.getKey(), ""));
                                                msg.setProductId(values.getString(DeviceMessageConfigKey.productId.getKey(), ""));
                                                msg.setTenantId(values.getString(DeviceMessageConfigKey.tenantId.getKey(), ""));
                                                msg.setDebug((Integer) values.getNumber(DeviceMessageConfigKey.testFlag.getKey(), 0));
                                                boolean result = kafkaProducer.send(deviceTopic, msg);
                                                log.info("{} 发送消息:{} 内容：{}", deviceTopic, result, JSON.toJSONString(message));
                                                return Mono.just(result);
                                            })
                            );
//                            .flatMap(msg -> cache.getSelfConfig(DeviceMessageConfigKey.deviceType).defaultIfEmpty("").map(v -> {
//                                msg.setDevType(v);
//                                return msg;
//                            }))
//                            .flatMap(msg -> cache.getSelfConfig(DeviceMessageConfigKey.period).defaultIfEmpty("").map(v -> {
//                                msg.setPeriod(v);
//                                return msg;
//                            }))
//                            .flatMap(msg -> cache.getSelfConfig(DeviceMessageConfigKey.deviceName).defaultIfEmpty("").map(v -> {
//                                msg.setDeviceName(v);
//                                return msg;
//                            }))
//                            .flatMap(msg -> cache.getSelfConfig(DeviceMessageConfigKey.productId).defaultIfEmpty("").map(v -> {
//                                msg.setProductId(v);
//                                return msg;
//                            }))
//                            .flatMap(msg -> cache.getSelfConfig(DeviceMessageConfigKey.sn).defaultIfEmpty("").map(v -> {
//                                msg.setSn(v);
//                                return msg;
//                            }))
//                            .flatMap(msg -> cache.getSelfConfig(DeviceMessageConfigKey.tenantId).defaultIfEmpty("").map(v -> {
//                                msg.setTenantId(v);
//                                return msg;
//                            }))
//                            .flatMap(msg -> cache.getSelfConfig(DeviceMessageConfigKey.testFlag).defaultIfEmpty(0).map(v -> {
//                                msg.setDebug(v);
//                                return msg;
//                            }))
//                            .flatMap(msg -> {
//                                boolean result = kafkaProducer.send(deviceTopic, msg);
//                                log.info("{} 发送消息:{} 内容：{}", deviceTopic, result, JSON.toJSONString(message));
//                                return Mono.just(result);
//                            });

                });

    }

}
