package top.iot.gateway.manager.debug;

import top.iot.gateway.component.gateway.external.Message;
import top.iot.gateway.component.gateway.external.SubscribeRequest;
import top.iot.gateway.component.gateway.external.SubscriptionProvider;
import top.iot.gateway.manager.api.request.MqttMessageRequest;
import top.iot.gateway.manager.api.response.MqttMessageResponse;
import top.iot.gateway.network.DefaultNetworkType;
import top.iot.gateway.network.NetworkManager;
import top.iot.gateway.network.mqtt.client.MqttClient;
import org.hswebframework.web.bean.FastBeanCopier;
import top.iot.gateway.core.message.codec.SimpleMqttMessage;
import top.iot.gateway.core.utils.TopicUtils;
import top.iot.gateway.rule.engine.executor.PayloadType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.Map;

@Component
public class MqttClientDebugSubscriptionProvider implements SubscriptionProvider {

    private final NetworkManager networkManager;

    public MqttClientDebugSubscriptionProvider(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @Override
    public String id() {
        return "network-client-mqtt-debug";
    }

    @Override
    public String name() {
        return "MQTT客户端调试";
    }

    @Override
    public String[] getTopicPattern() {
        return new String[]{
            "/network/mqtt/client/*/_subscribe/*",
            "/network/mqtt/client/*/_publish/*"
        };
    }

    @Override
    public Flux<Object> subscribe(SubscribeRequest request) {
        DebugAuthenticationHandler.handle(request);
        Map<String, String> vars = TopicUtils.getPathVariables("/network/mqtt/client/{id}/{pubsub}/{type}", request.getTopic());

        String clientId = vars.get("id");
        String pubsub = vars.get("pubsub");
        PayloadType type = PayloadType.valueOf(vars.get("type").toUpperCase());

        return networkManager
            .<MqttClient>getNetwork(DefaultNetworkType.MQTT_CLIENT, clientId)
            .flatMapMany(mqtt ->
                "_subscribe".equals(pubsub)
                    ? mqttClientSubscribe(mqtt, type, request)
                    : mqttClientPublish(mqtt, type, request))
            ;
    }

    public Flux<Object> mqttClientSubscribe(MqttClient client,
                                            PayloadType type,
                                            SubscribeRequest request) {
        String topics = request.getString("topics", "/#");
        return client
            .subscribe(Arrays.asList(topics.split("[\n]")))
            .map(mqttMessage -> {
                MqttMessageResponse mqttMessageResponse = MqttMessageResponse.builder()
                    .dup(mqttMessage.isDup())
                    .payload(type.read(mqttMessage.getPayload()))
                    .messageId(mqttMessage.getMessageId())
                    .qosLevel(mqttMessage.getQosLevel())
                    .topic(mqttMessage.getTopic())
                    .build();
                return Message.success(request.getId(), request.getTopic(), mqttMessageResponse);
            });

    }

    public Flux<String> mqttClientPublish(MqttClient client,
                                          PayloadType type,
                                          SubscribeRequest request) {
        MqttMessageRequest messageRequest = FastBeanCopier.copy(request.values(), new MqttMessageRequest());

        SimpleMqttMessage message = FastBeanCopier.copy(messageRequest, new SimpleMqttMessage());
        message.setPayload(type.write(messageRequest.getData()));

        return client
            .publish(message)
            .thenReturn("推送成功")
            .flux();

    }
}
