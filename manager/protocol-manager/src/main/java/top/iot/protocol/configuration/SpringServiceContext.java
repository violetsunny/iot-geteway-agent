package top.iot.protocol.configuration;

import lombok.extern.slf4j.Slf4j;
import top.iot.gateway.core.Value;
import top.iot.gateway.core.config.ConfigKey;
import top.iot.gateway.core.spi.ServiceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class SpringServiceContext implements ServiceContext {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public Optional<Value> getConfig(ConfigKey<String> key) {
        return getConfig(key.getKey());
    }

    @Override
    public Optional<Value> getConfig(String key) {
        return Optional.ofNullable(applicationContext.getEnvironment()
                .getProperty(key))
                .map(Value::simple)
                ;
    }

    @Override
    public <T> Optional<T> getService(Class<T> service) {
        try {
            return Optional.of(applicationContext.getBean(service));
        } catch (Exception e) {
            log.error("load service [{}] error", service, e);
            return Optional.empty();
        }
    }

    @Override
    public <T> Optional<T> getService(String service) {
        try {
            return Optional.of((T)applicationContext.getBean(service));
        } catch (Exception e) {
            log.error("load service [{}] error", service, e);
            return Optional.empty();
        }
    }

    @Override
    public <T> List<T> getServices(Class<T> service) {
        try {
            return new ArrayList<>(applicationContext.getBeansOfType(service).values());
        }catch (Exception e){
            log.error("load service [{}] error", service, e);
            return Collections.emptyList();
        }
    }

    @Override
    public <T> List<T> getServices(String service) {
        return Collections.emptyList();
    }
}
