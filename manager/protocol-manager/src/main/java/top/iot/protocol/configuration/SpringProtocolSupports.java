package top.iot.protocol.configuration;

import top.iot.gateway.core.ProtocolSupports;
import top.iot.gateway.core.defaults.CompositeProtocolSupports;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;


@Component
@Primary
public class SpringProtocolSupports extends CompositeProtocolSupports implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object o, String s) throws BeansException {
        if (o == this) {
            return o;
        }
        if (o instanceof ProtocolSupports) {
            register(((ProtocolSupports) o));
        }
        return o;
    }
}
