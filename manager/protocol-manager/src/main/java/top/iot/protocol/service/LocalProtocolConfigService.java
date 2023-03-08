package top.iot.protocol.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import top.iot.protocol.entity.DeviceProtocolConfigEntity;
import top.iot.protocol.godson.metadataMapping.GatewayMetadataMapping;
import top.iot.protocol.godson.metadataMapping.ProtocolConfigRegistry;
import top.iot.protocol.web.request.ProtocolConfigDecodeRequest;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.hswebframework.utils.StringUtils;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import top.iot.gateway.core.message.codec.MessagePayloadType;
import top.iot.gateway.core.metadata.DeviceMetadataType;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class LocalProtocolConfigService extends GenericReactiveCrudService<DeviceProtocolConfigEntity, String> {

    private final ProtocolConfigRegistry registry;

//    private final ReactiveRepository<DeviceInstanceEntity, String> deviceInstanceRepository;

    public LocalProtocolConfigService(ProtocolConfigRegistry registry) {
        this.registry = registry;
    }

    public Mono<DeviceProtocolConfigEntity> _save(DeviceProtocolConfigEntity payload) {
        return Mono.just(payload).flatMap(p ->
            save(Mono.just(payload)).flatMap(__ -> deploy(payload))
        );
    }

    public Mono<DeviceProtocolConfigEntity> deploy(DeviceProtocolConfigEntity payload) {
        return registry.register(payload.toProtocolConfigInfo())
            .thenReturn(payload);
    }

    @Override
    public Mono<Integer> deleteById(Publisher<String> idPublisher) {
        return Mono.empty();
        //todo 调用设备管理服务查询该协议模型下是否有已绑定的设备，如果没有再删除和注销
//        return Flux.from(idPublisher)
//            .collectList()
//            .flatMap(idList ->
//                deviceInstanceRepository.createQuery().where().in(DeviceInstanceEntity::getProductId, idList).count().flatMap(res -> {
//                    if(res > 0){
//                        return Mono.error(new BusinessException("该协议模型已绑定设备,无法删除"));
//                    }
//                    return super.deleteById(idPublisher).flatMap(r -> undeploy(idPublisher).thenReturn(r));
//                })
//            );
    }

    private Mono<Integer> undeploy(Publisher<String> idPublisher) {
        return Flux.from(idPublisher)
            .flatMap(registry::unregister).count().map(Long::intValue);
    }


    //todo 订阅物模型中的属性创建、修改、删除操作，保存协议模型数据
//    @EventListener
//    public void handleEvent(EntityModifyEvent<DeviceProductEntity> entity) {
//        entity.getAfter()
//            .stream()
//            .filter(DeviceProductEntity::isSyncProtocolConfig)
//            .forEach(this::handleEvent);
//    }
//
//    @EventListener
//    public void handleEvent(EntityCreatedEvent<DeviceProductEntity> entity) {
//        log.info("product created event start");
//    }
//
//    @EventListener
//    public void handleEvent(EntitySavedEvent<DeviceProductEntity> entity) {
//        log.info("product saved event start");
//        entity.getEntity().forEach(this::handleEvent);
//    }
//
//    private void handleEvent(DeviceProductEntity product) {
//        String metadata = product.getMetadata();
//        if (Strings.isNotEmpty(metadata)) {
//            JSONObject metadataJsonObject = JSON.parseObject(metadata);
//            JSONArray properties = metadataJsonObject.getJSONArray("properties");
//            JSONArray events = metadataJsonObject.getJSONArray("events");
//            if (properties.size()>0 || events.size()>0) {
//                IotGatewayDeviceMetadata deviceMetadata = new IotGatewayDeviceMetadata(JSON.parseObject(metadata));
//                findById(product.getId())
//                    .switchIfEmpty(
//                        Mono.defer(() -> {
//                            DeviceProtocolConfigEntity config = new DeviceProtocolConfigEntity();
//                            config.setId(product.getId());
//                            return Mono.justOrEmpty(config);
//                        }))
//                    .flatMap(config -> {
//                        IotGatewayDeviceMetadataMapping another = new IotGatewayDeviceMetadataMapping(JSONObject.parseObject(JSONObject.toJSONString(config)));
//                        String metadataMapping = JSONObject.toJSONString(new IotGatewayDeviceMetadataMapping(deviceMetadata, another).toJson());
//                        config.setMetadataMapping(metadataMapping);
//                        return Mono.justOrEmpty(config);
//                    }).flatMap(this::save)
//                    .subscribe(r -> log.info("更新协议模型成功"));
//            } else {
//                findById(product.getId()).doOnNext(entity -> {
//                    if (entity != null) {
//                        deleteById(Mono.just(product.getId()));
//                    }
//                }).subscribe();
//            }
//        } else {
//            findById(product.getId()).doOnNext(entity -> {
//                if (entity != null) {
//                    deleteById(Mono.just(product.getId()));
//                }
//            }).subscribe();
//        }
//    }

    public Mono<String> decode(Mono<ProtocolConfigDecodeRequest> entity) {
        return entity.map(request -> {
            Object payload;
            switch (request.getPayloadType()) {
                case STRING:
                    payload = request.getPayload().getBytes();
                    break;
                case BINARY:
                    payload = Hex.decode(request.getPayload());
                    break;
                default:
                    payload = request.getPayload();
                    break;
            }
            return registry.getMessagePayloadParser(request.getPayloadType())
                .parseExpressions(JSONArray.parseArray(request.getMetadataMappings(), GatewayMetadataMapping.class), payload, request.getDeviceMetadataType());
        })
            .map(JSON::toJSONString)
            .onErrorResume(err -> Mono.just(StringUtils.throwable2String(err)));
    }

    public Mono<String> parameterDecode(Mono<ProtocolConfigDecodeRequest> entity) {
        return entity.map(request -> {
            if (request.getPayload().startsWith("[") || request.getPayload().startsWith("{")) {
                return registry.getMessagePayloadParser(MessagePayloadType.JSON)
                    .parseExpressions(JSONArray.parseArray(request.getMetadataMappings(), GatewayMetadataMapping.class), request.getPayload(), DeviceMetadataType.property);
            } else {
                return registry.getMessagePayloadParser(MessagePayloadType.STRING)
                    .parseParamExpressions(JSONArray.parseArray(request.getMetadataMappings(), GatewayMetadataMapping.class), request.getPayload(), DeviceMetadataType.property);
            }
        })
            .map(JSON::toJSONString)
            .onErrorResume(err -> Mono.just(StringUtils.throwable2String(err)));
    }
}
