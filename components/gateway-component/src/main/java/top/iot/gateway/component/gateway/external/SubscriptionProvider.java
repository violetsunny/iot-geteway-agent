package top.iot.gateway.component.gateway.external;

import reactor.core.publisher.Flux;

public interface SubscriptionProvider {

    String id();

    String name();

    String[] getTopicPattern();

    Flux<?> subscribe(SubscribeRequest request);

}
