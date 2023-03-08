package top.iot.gateway.component.gateway.spring;

import top.iot.gateway.core.event.TopicPayload;
import reactor.core.publisher.Mono;

public interface MessageListener {

    Mono<Void> onMessage(TopicPayload message);

}
