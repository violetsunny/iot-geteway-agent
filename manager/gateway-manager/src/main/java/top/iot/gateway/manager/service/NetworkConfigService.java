package top.iot.gateway.manager.service;

import top.iot.gateway.manager.entity.NetworkConfigEntity;
import top.iot.gateway.manager.enums.NetworkConfigState;
import top.iot.gateway.network.NetworkConfigManager;
import top.iot.gateway.network.NetworkProperties;
import top.iot.gateway.network.NetworkType;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author hanyl
 * @since 1.0
 **/
@Service
public class NetworkConfigService extends GenericReactiveCrudService<NetworkConfigEntity, String> implements NetworkConfigManager {

    @Override
    public Mono<NetworkProperties> getConfig(NetworkType networkType, String id) {
        return findById(id)
                .map(NetworkConfigEntity::toNetworkProperties);
    }

    @Override
    public Mono<SaveResult> save(Publisher<NetworkConfigEntity> entityPublisher) {
        return super.save(
            Flux.from(entityPublisher)
                .doOnNext(entity -> {
                    if (StringUtils.isEmpty(entity.getId())) {
                        entity.setState(NetworkConfigState.disabled);
                    } else {
                        entity.setState(null);
                    }
                }));
    }

    @Override
    public Mono<Integer> insert(Publisher<NetworkConfigEntity> entityPublisher) {
        return super.insert(
                Flux.from(entityPublisher)
                        .doOnNext(entity -> entity.setState(NetworkConfigState.disabled)));
    }
}
