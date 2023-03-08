/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.iot.gateway.component.elasticsearch.service;

import com.alibaba.fastjson.JSON;
import top.iot.gateway.component.common.utils.SystemUtils;
import top.iot.gateway.component.elasticsearch.configuration.EsBufferConfig;
import io.netty.util.internal.ObjectPool;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hswebframework.web.bean.FastBeanCopier;
import top.iot.gateway.core.utils.FluxUtils;
import org.reactivestreams.Publisher;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;
import reactor.core.publisher.BufferOverflowStrategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * iotES操作
 *
 * @author kanglele
 * @version $Id: IotReactiveElasticSearchService, v 0.1 2023/1/12 9:58 kanglele Exp $
 */
@Service("iotReactiveElasticSearchService")
@Slf4j
@DependsOn("reactiveElasticsearchClient")
public class IotReactiveElasticSearchService implements ElasticSearchService {

    @Resource
    private ReactiveElasticsearchOperations reactiveElasticsearchOperations;

    private final EsBufferConfig buffer;

    private FluxSink<EsBuffer> sink;

//    public static final IndicesOptions indexOptions = IndicesOptions.fromOptions(
//            true, true, false, false
//    );

//    {
//        DateFormatter.supportFormatter.add(new DefaultDateFormatter(Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.+"), "yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
//    }

    private IotReactiveElasticSearchService(EsBufferConfig buffer) {
        this.buffer = buffer;
        init();
    }

    public void init() {
        int flushRate = buffer.getRate();
        int bufferSize = buffer.getBufferSize();
        Duration bufferTimeout = buffer.getBufferTimeout();
        int bufferBackpressure = buffer.getBufferBackpressure();
        long bufferBytes = buffer.getBufferBytes().toBytes();
        AtomicLong bufferedBytes = new AtomicLong();

        FluxUtils
                //建立缓冲池
                .bufferRate(Flux.<EsBuffer>create(sink -> this.sink = sink),
                        flushRate,
                        bufferSize,
                        bufferTimeout,
                        (b, l) -> bufferedBytes.addAndGet(b.numberOfBytes()) >= bufferBytes)
                .doOnNext(buf -> bufferedBytes.set(0))
                //进入背压器
                .onBackpressureBuffer(bufferBackpressure, drop -> {
                    log.error("elasticsearch无法处理更多索引请求!丢弃数据数量:{}", JSON.toJSONString(drop));
                    drop.forEach(EsBuffer::release);
                }, BufferOverflowStrategy.DROP_OLDEST)
                //异步调度
                .publishOn(Schedulers.boundedElastic(), bufferBackpressure)
                .flatMap(buffers -> {
                    return Mono.create(sink -> {
                        try {
                            sink.onCancel(this
                                    .doSave(buffers)
                                    .doFinally((s) -> sink.success())
                                    .subscribe());
                        } catch (Exception e) {
                            sink.success();
                        }
                    });
                })
                .onErrorResume((err) -> Mono
                        .fromRunnable(() -> log.error("保存ElasticSearch数据失败", err)))
                .subscribe();
    }

    private Mono<Integer> doSave(List<EsBuffer> buffers) {
        return Flux
                .fromIterable(buffers)
                .groupBy(EsBuffer::getIndex, Integer.MAX_VALUE)
                .flatMap(group -> {
                    String index = group.key();
                    return this
                            .wrapIndex(index)
                            .flatMapMany(realIndex ->
                                    saveIndexAll(realIndex, group.map(EsBuffer::getPayload).collectList())
                                            .as(save -> {
                                                if (buffer.getMaxRetry() > 0) {
                                                    return save.retryWhen(Retry.backoff(buffer.getMaxRetry(), buffer.getMinBackoff()));
                                                }
                                                return save;
                                            })
                            );
                })
                .collectList()
                .filter(CollectionUtils::isNotEmpty)
                .doOnError((err) -> {
                    log.error("保存ElasticSearch数据失败", err);
                })
                .doOnSuccess((res) -> {
                    log.info("保存ElasticSearch数据成功 id:{}",
                            StringUtils.join(res.stream().map(oj -> {
                                if (oj instanceof Map && ((Map)oj).containsKey("id")) {
                                    return ((Map)oj).get("id");
                                } else {
                                    return "";
                                }
                            }).collect(Collectors.toList()), ","));
                })
                .thenReturn(buffers.size());
    }

    @Override
    public <T> Mono<Void> commit(String index, Publisher<T> data) {
        if (!checkWritable(index)) {
            return Mono.empty();
        }
        return Flux.from(data)
                .flatMap(d -> commit(index, d))
                .then();
    }

    @Override
    public <T> Mono<Void> commit(String index, T payload) {
        if (checkWritable(index)) {
            sink.next(EsBuffer.of(index, payload));
        }
        return Mono.empty();
    }

    @Override
    public <T> Mono<Void> commit(String index, List<T> payloads) {
        if (checkWritable(index)) {
            for (T payload : payloads) {
                sink.next(EsBuffer.of(index, payload));
            }
        }
        return Mono.empty();
    }

    private boolean checkWritable(String index) {
        if (SystemUtils.memoryIsOutOfWatermark()) {
            log.error("JVM内存不足,elasticsearch无法处理更多索引{}请求!", index);
            return false;
        }
        return true;
    }

    public <T> Flux<T> saveIndexAll(String index, Mono<List<T>> payloads) {
        IndexCoordinates indexCoordinates = IndexCoordinates.of(index);
        return reactiveElasticsearchOperations.saveAll(payloads, indexCoordinates);
    }

    private Mono<String> wrapIndex(String index) {
        return Mono.just(index.toLowerCase());
    }


    //使用对象池处理Buffer,减少GC消耗
    static ObjectPool<EsBuffer> pool = ObjectPool.newPool(EsBuffer::new);

    @Getter
    static class EsBuffer {
        final ObjectPool.Handle<EsBuffer> handle;
        String index;
        String id;
        Object payload;

        public EsBuffer(ObjectPool.Handle<EsBuffer> handle) {
            this.handle = handle;
        }

        public static EsBuffer of(String index, Object payload) {
            EsBuffer buffer;
            try {
                buffer = pool.get();
            } catch (Exception e) {
                buffer = new EsBuffer(null);
            }
            buffer.index = index;
            Map<String, Object> data = payload instanceof Map
                    ? ((Map) payload) :
                    FastBeanCopier.copy(payload, HashMap::new);
            Object id = data.get("id");
//            Object deviceId = data.get("deviceId");
//            buffer.id = id == null ? deviceId + "_" + UUID.randomUUID().toString().toUpperCase().replaceAll("-", "") : String.valueOf(id);
            buffer.id = id == null ? null : String.valueOf(id);
            buffer.payload = data;
            return buffer;
        }

        void release() {
            this.index = null;
            this.id = null;
            this.payload = null;
            if (null != handle) {
                handle.recycle(this);
            }
        }

        int numberOfBytes() {
            return payload == null ? 0 : JSON.toJSONString(payload).length() * 2;
        }
    }

}
