package top.iot.gateway.logging.event.handler;

import top.iot.gateway.component.elasticsearch.service.ElasticSearchService;
import top.iot.gateway.logging.access.SerializableAccessLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * @author bsetfeng
 * @since 1.0
 **/
@Component
@Slf4j
@Order(5)
public class AccessLoggerEventHandler {

    private final ElasticSearchService elasticSearchService;


    public AccessLoggerEventHandler(ElasticSearchService elasticSearchService) {
        this.elasticSearchService = elasticSearchService;
//        indexManager.putIndex(
//            new DefaultElasticSearchIndexMetadata(LoggerIndexProvider.ACCESS.getIndex())
//                .addProperty("requestTime", new DateTimeType())
//                .addProperty("responseTime", new DateTimeType())
//                .addProperty("action", new StringType())
//                .addProperty("ip", new StringType())
//                .addProperty("url", new StringType())
//                .addProperty("httpHeaders", new ObjectType())
//                .addProperty("context", new ObjectType()
//                    .addProperty("userId",new StringType())
//                    .addProperty("username",new StringType())
//                )
//        ).subscribe();

    }


    @EventListener
    public void acceptAccessLoggerInfo(SerializableAccessLog info) {
        elasticSearchService.commit(LoggerIndexProvider.ACCESS, Mono.just(info)).subscribe();
    }

}
