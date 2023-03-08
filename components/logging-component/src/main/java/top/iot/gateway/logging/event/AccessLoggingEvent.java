package top.iot.gateway.logging.event;

import top.iot.gateway.logging.access.SerializableAccessLog;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AccessLoggingEvent {
    SerializableAccessLog log;
}
