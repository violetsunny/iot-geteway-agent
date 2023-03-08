package top.iot.gateway.component.elasticsearch.service;

import top.iot.gateway.component.elasticsearch.index.ElasticIndex;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ElasticSearchService {
    default <T> Mono<Void> commit(ElasticIndex index, Publisher<T> data) {
        return this.commit(index.getIndex(), data);
    }

    <T> Mono<Void> commit(String index, Publisher<T> data);

    <T> Mono<Void> commit(String index, T payload);

    <T> Mono<Void> commit(String index, List<T> payloads);
}
