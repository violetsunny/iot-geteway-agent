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
@Tag(name = "????????????")
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
            summary = "??????ID????????????"
    )
    public Mono<ProtocolSupportEntity> getById(@PathVariable String id) {
        return this.getService().findById(id).switchIfEmpty(Mono.empty());
    }

    @PostMapping("/save")
    @SaveAction
    @Operation(
            summary = "??????????????????",
            description = "???????????????id,????????????????????????,???????????????,??????????????????."
    )
    public Mono<SaveResult> saveProtocol(@RequestBody Mono<ProtocolSupportEntity> payload) {
        //??????ID???????????????????????????????????????~!@#$%^&*()-_+<>?
        Mono<ProtocolSupportEntity> payload1 = payload.switchIfEmpty(ErrorUtils.notFound("?????????????????????"))
                .filter(p -> RegexUtil.validateId(p.getId()))
                .switchIfEmpty(Mono.error(() -> new BusinessException("id???????????????????????????")))
                .map(p -> {p.setUpdateTime(new Date());return p;});
        return save(Flux.from(payload1));
    }

    @PostMapping("/{id}/_deploy")
    @SaveAction
    @Operation(summary = "????????????")
    public Mono<Boolean> deploy(@PathVariable String id) {
        return service.deploy(id);
    }

    @PostMapping("/{id}/_un-deploy")
    @SaveAction
    @Operation(summary = "????????????")
    public Mono<Boolean> unDeploy(@PathVariable String id) {
        return service.unDeploy(id);
    }

    //???????????????????????????
    @GetMapping("/providers")
    @Authorize(merge = false)
    @Operation(summary = "?????????????????????????????????")
    public Flux<String> getProviders() {
        return Flux
                .fromIterable(providers)
                .map(ProtocolSupportLoaderProvider::getProvider);
    }



    @GetMapping("/allSupports")
    @Authorize(merge = false)
    @Operation(summary = "?????????????????????????????????")
    public Flux<ProtocolInfo> allProtocols() {
        Flux<ProtocolInfo> protocolInfos = protocolSupports.getProtocols().mapNotNull(ProtocolInfo::of);
        Flux<ProtocolInfo> protocolInfoTwo = service.queryListSate(null).mapNotNull(ProtocolInfo::of);
        return protocolInfos.mergeWith(protocolInfoTwo);
    }

    @GetMapping("/supports")
    @Authorize(merge = false)
    @Operation(summary = "???????????????????????????????????????")
    public Flux<ProtocolInfo> allProtocols(@RequestParam(required = false)
                                           @Parameter(description = "0-?????????,1-?????????,null-?????????") Integer state) {
//        Flux<ProtocolInfo> protocolInfos = protocolSupports.getProtocols().mapNotNull(ProtocolInfo::of);
//        Flux<ProtocolInfo> protocolInfoTwo = service.queryListSate(state).mapNotNull(ProtocolInfo::of);
//        return protocolInfos.mergeWith(protocolInfoTwo);
        //??????protocolSupports.getProtocols()????????????????????????????????????redis??????????????????????????????????????????redis???
        //??????service.queryList??????????????????????????????????????????????????????????????????????????????
        switch (state){
            case 0:
                return service.queryListSate(state).mapNotNull(ProtocolInfo::of);
            case 1:
                //?????????????????????????????????????????????????????????????????????protocolSupports???????????????
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
    @Operation(summary = "??????????????????????????????????????????????????????")
    public Mono<ConfigMetadata> getTransportConfiguration(@PathVariable @Parameter(description = "??????ID") String id,
                                                          @PathVariable @Parameter(description = "????????????") String transport) {
        return protocolSupports.getProtocol(id)
                .flatMap(support -> support.getConfigMetadata(Transport.of(transport)));
    }

    @GetMapping("/{id}/{transport}/metadata")
    @QueryAction
    @Authorize(merge = false)
    @Operation(summary = "????????????????????????????????????")
    public Mono<String> getDefaultMetadata(@PathVariable @Parameter(description = "??????ID") String id,
                                           @PathVariable @Parameter(description = "????????????") String transport) {
        return protocolSupports
                .getProtocol(id)
                .flatMap(support -> support
                        .getDefaultMetadata(Transport.of(transport))
                        .flatMap(metadata -> support.getMetadataCodec().encode(metadata))
                ).defaultIfEmpty("{}");
    }

    @GetMapping("/{id}/transports")
    @Authorize(merge = false)
    @Operation(summary = "?????????????????????????????????")
    public Flux<TransportInfo> getAllTransport(@PathVariable @Parameter(description = "??????ID") String id) {
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
    @Operation(summary = "??????????????????")
    public Flux<ValueUnit> allUnits() {
        return Flux.fromIterable(ValueUnits.getAllUnit());
    }
}
