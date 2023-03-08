package top.iot.protocol.web;

import com.alibaba.fastjson.JSON;
import top.iot.gateway.component.common.utils.ErrorUtils;
import top.iot.gateway.component.common.utils.RegexUtil;
import top.iot.protocol.entity.ProtocolSupportEntity;
import top.iot.protocol.service.LocalProtocolSupportService;
import top.iot.protocol.web.request.ProtocolDecodeRequest;
import top.iot.protocol.web.request.ProtocolEncodeRequest;
import top.iot.protocol.web.response.ProtocolDetail;
import top.iot.protocol.web.response.ProtocolInfo;
import top.iot.protocol.web.response.TransportInfo;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.utils.StringUtils;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.BusinessException;
import top.iot.gateway.core.ProtocolSupport;
import top.iot.gateway.core.ProtocolSupports;
import top.iot.gateway.core.message.codec.Transport;
import top.iot.gateway.core.metadata.ConfigMetadata;
import top.iot.gateway.core.metadata.unit.ValueUnit;
import top.iot.gateway.core.metadata.unit.ValueUnits;
import top.iot.gateway.supports.protocol.management.ProtocolSupportDefinition;
import top.iot.gateway.supports.protocol.management.ProtocolSupportLoader;
import top.iot.gateway.supports.protocol.management.ProtocolSupportLoaderProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/protocol")
@Authorize(ignore = true)
@Tag(name = "协议管理")
@Slf4j
public class ProtocolSupportController
        implements ReactiveServiceCrudController<ProtocolSupportEntity, String> {

    @Autowired
    @Getter
    private LocalProtocolSupportService service;

    @Autowired
    private ProtocolSupports protocolSupports;

    @Autowired
    private List<ProtocolSupportLoaderProvider> providers;

    @Autowired
    private ProtocolSupportLoader supportLoader;

    @GetMapping({"/{id}"})
    @QueryAction
    @Operation(
            summary = "根据ID查询协议"
    )
    public Mono<ProtocolSupportEntity> getById(@PathVariable String id) {
        return this.getService().findById(id).switchIfEmpty(Mono.empty());
    }

    @PostMapping("/save")
    @SaveAction
    @Operation(
            summary = "保存协议数据",
            description = "如果传入了id,并且对应数据存在,则尝试覆盖,不存在则新增."
    )
    public Mono<SaveResult> saveProtocol(@RequestBody Mono<ProtocolSupportEntity> payload) {
        //协议ID规则，只能英文、数字、字符~!@#$%^&*()-_+<>?
        Mono<ProtocolSupportEntity> payload1 = payload.switchIfEmpty(ErrorUtils.notFound("协议数据不存在"))
                .filter(p -> RegexUtil.validateId(p.getId()))
                .switchIfEmpty(Mono.error(() -> new BusinessException("id输入了不支持的字符")))
                .map(p -> {p.setUpdateTime(new Date());return p;});
        return save(Flux.from(payload1));
    }

    @PostMapping("/{id}/_deploy")
    @SaveAction
    @Operation(summary = "发布协议")
    public Mono<Boolean> deploy(@PathVariable String id) {
        return service.deploy(id);
    }

    @PostMapping("/{id}/_un-deploy")
    @SaveAction
    @Operation(summary = "取消发布")
    public Mono<Boolean> unDeploy(@PathVariable String id) {
        return service.unDeploy(id);
    }

    //获取支持的协议类型
    @GetMapping("/providers")
    @Authorize(merge = false)
    @Operation(summary = "获取当前支持的协议类型")
    public Flux<String> getProviders() {
        return Flux
                .fromIterable(providers)
                .map(ProtocolSupportLoaderProvider::getProvider);
    }



    @GetMapping("/allSupports")
    @Authorize(merge = false)
    @Operation(summary = "获取当前支持的所有协议")
    public Flux<ProtocolInfo> allProtocols() {
        Flux<ProtocolInfo> protocolInfos = protocolSupports.getProtocols().mapNotNull(ProtocolInfo::of);
        Flux<ProtocolInfo> protocolInfoTwo = service.queryListSate(null).mapNotNull(ProtocolInfo::of);
        return protocolInfos.mergeWith(protocolInfoTwo);
    }

    @GetMapping("/supports")
    @Authorize(merge = false)
    @Operation(summary = "获取当前支持的指定状态协议")
    public Flux<ProtocolInfo> allProtocols(@RequestParam(required = false)
                                           @Parameter(description = "0-未发布,1-已发布,null-查所有") Integer state) {
//        Flux<ProtocolInfo> protocolInfos = protocolSupports.getProtocols().mapNotNull(ProtocolInfo::of);
//        Flux<ProtocolInfo> protocolInfoTwo = service.queryListSate(state).mapNotNull(ProtocolInfo::of);
//        return protocolInfos.mergeWith(protocolInfoTwo);
        //通过protocolSupports.getProtocols()取得的协议包括内置协议，redis中的协议（库中协议发布后存入redis）
        //通过service.queryList取得的是库中的协议，其中已发布协议会与上一项中的重复
        switch (state){
            case 0:
                return service.queryListSate(state).mapNotNull(ProtocolInfo::of);
            case 1:
                //已发布协议在服务初始化时都已加载在内存中，通过protocolSupports的方法获取
                return protocolSupports.getProtocols()
                        .filter(p-> !p.getId().equals("iot-gateway.v1.0"))
                        .mapNotNull(ProtocolInfo::of);
            default:
                Flux<ProtocolInfo> protocolInfos = protocolSupports.getProtocols()
                        .filter(p-> !p.getId().equals("iot-gateway.v1.0"))
                        .mapNotNull(ProtocolInfo::of);
                Flux<ProtocolInfo> protocolInfoTwo = service.queryListSate(state).mapNotNull(ProtocolInfo::of);
                return protocolInfos.mergeWith(protocolInfoTwo).distinct();
        }

    }

    @GetMapping("/{id}/{transport}/configuration")
    @QueryAction
    @Authorize(merge = false)
    @Operation(summary = "获取协议对应使用传输协议的配置元数据")
    public Mono<ConfigMetadata> getTransportConfiguration(@PathVariable @Parameter(description = "协议ID") String id,
                                                          @PathVariable @Parameter(description = "传输协议") String transport) {
        return protocolSupports.getProtocol(id)
                .flatMap(support -> support.getConfigMetadata(Transport.of(transport)));
    }

    @GetMapping("/{id}/{transport}/metadata")
    @QueryAction
    @Authorize(merge = false)
    @Operation(summary = "获取协议设置的默认物模型")
    public Mono<String> getDefaultMetadata(@PathVariable @Parameter(description = "协议ID") String id,
                                           @PathVariable @Parameter(description = "传输协议") String transport) {
        return protocolSupports
                .getProtocol(id)
                .flatMap(support -> support
                        .getDefaultMetadata(Transport.of(transport))
                        .flatMap(metadata -> support.getMetadataCodec().encode(metadata))
                ).defaultIfEmpty("{}");
    }

    @GetMapping("/{id}/transports")
    @Authorize(merge = false)
    @Operation(summary = "获取协议支持的传输协议")
    public Flux<TransportInfo> getAllTransport(@PathVariable @Parameter(description = "协议ID") String id) {
        return protocolSupports
                .getProtocol(id)
                .flatMapMany(ProtocolSupport::getSupportedTransport)
                .distinct()
                .map(TransportInfo::of);
    }

    @PostMapping("/convert")
    @QueryAction
    @Hidden
    public Mono<ProtocolDetail> convertToDetail(@RequestBody Mono<ProtocolSupportEntity> entity) {
        return entity.map(ProtocolSupportEntity::toDeployDefinition)
                .doOnNext(def -> def.setId("_debug"))
                .flatMap(def -> supportLoader.load(def))
                .flatMap(ProtocolDetail::of);
    }

    @PostMapping("/decode")
    @SaveAction
    @Hidden
    public Mono<String> decode(@RequestBody Mono<ProtocolDecodeRequest> entity) {
        return entity
                .<Object>flatMapMany(request -> {
                    ProtocolSupportDefinition supportEntity = request.getEntity().toDeployDefinition();
                    supportEntity.setId("_debug");
                    return supportLoader.load(supportEntity)
                            .flatMapMany(protocol -> request
                                    .getRequest()
                                    .doDecode(protocol, null));
                })
                .collectList()
                .map(JSON::toJSONString)
                .onErrorResume(err -> Mono.just(StringUtils.throwable2String(err)));
    }

    @PostMapping("/encode")
    @SaveAction
    @Hidden
    public Mono<String> encode(@RequestBody Mono<ProtocolEncodeRequest> entity) {
        return entity
                .flatMapMany(request -> {
                    ProtocolSupportDefinition supportEntity = request.getEntity().toDeployDefinition();
                    supportEntity.setId("_debug");
                    return supportLoader.load(supportEntity)
                            .flatMapMany(protocol -> request
                                    .getRequest()
                                    .doEncode(protocol, null));
                })
                .collectList()
                .map(JSON::toJSONString)
                .onErrorResume(err -> Mono.just(StringUtils.throwable2String(err)));
    }

    @GetMapping("/units")
    @Operation(summary = "获取单位数据")
    public Flux<ValueUnit> allUnits() {
        return Flux.fromIterable(ValueUnits.getAllUnit());
    }
}
