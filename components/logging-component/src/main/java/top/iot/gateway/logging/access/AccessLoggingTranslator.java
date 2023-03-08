package top.iot.gateway.logging.access;

import top.iot.gateway.logging.configuration.LoggingProperties;
import org.hswebframework.web.logging.events.AccessLoggerAfterEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AccessLoggingTranslator {

    private final ApplicationEventPublisher eventPublisher;

    private final LoggingProperties properties;

    public AccessLoggingTranslator(ApplicationEventPublisher eventPublisher, LoggingProperties properties) {
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @EventListener
    public void translate(AccessLoggerAfterEvent event) {
        eventPublisher.publishEvent(SerializableAccessLog.of(event.getLogger()));
    }

}
