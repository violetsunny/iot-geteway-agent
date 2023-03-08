package top.iot.gateway.network.mqtt.executor;

import top.iot.gateway.network.DefaultNetworkType;
import top.iot.gateway.network.NetworkManager;
import top.iot.gateway.network.PubSubType;
import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.dict.EnumDict;
import top.iot.gateway.network.mqtt.client.MqttClient;
import top.iot.gateway.core.message.codec.MqttMessage;
import top.iot.gateway.rule.engine.api.RuleConstants;
import top.iot.gateway.rule.engine.api.RuleData;
import top.iot.gateway.rule.engine.api.RuleDataCodecs;
import top.iot.gateway.rule.engine.api.RuleDataHelper;
import top.iot.gateway.rule.engine.api.task.ExecutionContext;
import top.iot.gateway.rule.engine.api.task.Task;
import top.iot.gateway.rule.engine.api.task.TaskExecutor;
import top.iot.gateway.rule.engine.api.task.TaskExecutorProvider;
import top.iot.gateway.rule.engine.defaults.AbstractTaskExecutor;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Component
public class MqttClientTaskExecutorProvider implements TaskExecutorProvider {

    private final NetworkManager networkManager;

    static {
        MqttRuleDataCodec.load();
    }

    @Override
    public String getExecutor() {
        return "mqtt-client";
    }

    protected Flux<MqttMessage> convertMessage(RuleData message, MqttClientTaskConfiguration config) {

        return RuleDataCodecs.getCodec(MqttMessage.class)
            .map(codec ->
                codec.decode(message,
                    config.getPayloadType(),
                    new MqttTopics(config.getTopics(RuleDataHelper.toContextMap(message))))
                    .cast(MqttMessage.class))
            .orElseThrow(() -> new UnsupportedOperationException("unsupported decode message:{}" + message));
    }

    protected Mono<RuleData> convertMessage(MqttMessage message, MqttClientTaskConfiguration config) {

        return Mono.just(RuleDataCodecs.getCodec(MqttMessage.class)
            .map(codec -> codec.encode(message, config.getPayloadType(), new TopicVariables(config.getTopicVariables())))
            .map(RuleData::create)
            .orElseGet(() -> RuleData.create(message)));
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new MqttClientTaskExecutor(context));
    }

    class MqttClientTaskExecutor extends AbstractTaskExecutor {

        private MqttClientTaskConfiguration config;

        public MqttClientTaskExecutor(ExecutionContext context) {
            super(context);
            reload();
        }

        @Override
        public String getName() {
            return "MQTT Client";
        }

        @Override
        public void reload() {
            config = FastBeanCopier.copy(context.getJob().getConfiguration(), new MqttClientTaskConfiguration());
            config.validate();
            if (disposable != null) {
                disposable.dispose();
            }
        }

        @Override
        public void validate() {
            FastBeanCopier
                .copy(context.getJob().getConfiguration(), new MqttClientTaskConfiguration())
                .validate();
        }

        @Override
        protected Disposable doStart() {
            Disposable.Composite disposable = Disposables.composite();

            if (EnumDict.in(PubSubType.producer, config.getClientType())) {
                disposable.add(context.getInput()
                    .accept()
                    .filter((data) -> state == Task.State.running)
                    .flatMap(data ->
                        networkManager
                            .<MqttClient>getNetwork(DefaultNetworkType.MQTT_CLIENT, config.getClientId())
                            .flatMapMany(client -> convertMessage(data, config)
                                .flatMap(msg -> client
                                    .publish(msg)
                                    .doOnSuccess((v) -> context.getLogger().debug("推送MQTT[{}]消息:{}", client.getId(), msg))
                                )
                            ).onErrorContinue((err, e) -> context.onError(err, null).subscribe())
                    )
                        //TODO 数据转发 data-iot-devType-{devType} send kafka
                    .subscribe()
                );
            }
            if (EnumDict.in(PubSubType.consumer, config.getClientType())) {
                disposable.add(networkManager
                    .<MqttClient>getNetwork(DefaultNetworkType.MQTT_CLIENT, config.getClientId())
                    .flatMapMany(client -> client.subscribe(config.getTopics()))
                    .filter((data) -> state == Task.State.running)
                    .doOnNext(message -> context.getLogger().info("consume mqtt message:{}", message))
                    .flatMap(message -> convertMessage(message, config))
                    .flatMap(ruleData -> context.getOutput().write(Mono.just(ruleData)).thenReturn(ruleData))
                    .flatMap(ruleData -> context.fireEvent(RuleConstants.Event.result, ruleData).thenReturn(ruleData))
                    .onErrorContinue((err, e) -> context.onError(err, null).subscribe())
                        //TODO 数据转发 data-iot-devType-{devType} send kafka
                    .subscribe());
            }
            return disposable;
        }
    }
}
