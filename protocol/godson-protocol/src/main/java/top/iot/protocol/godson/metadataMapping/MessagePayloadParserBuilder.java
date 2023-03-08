package top.iot.protocol.godson.metadataMapping;

import top.iot.gateway.core.message.codec.MessagePayloadType;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MessagePayloadParserBuilder implements BeanPostProcessor {

    private Map<MessagePayloadType, MessagePayloadParser> strategyMap = new ConcurrentHashMap<>();

    public MessagePayloadParser build(MessagePayloadType type) {
        return Optional.ofNullable(strategyMap.get(type))
            .orElseThrow(() -> new UnsupportedOperationException("unsupported parser:" + type));
    }

    public void register(MessagePayloadParser parser) {
        strategyMap.put(parser.getType(), parser);
    }

    @Override
    public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) throws BeansException {
        if (bean instanceof MessagePayloadParser) {
            register((MessagePayloadParser) bean);
        }
        return bean;
    }
}
