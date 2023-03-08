package top.iot.gateway.logging.event;

import top.iot.gateway.logging.system.SerializableSystemLog;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SystemLoggingEvent {
    SerializableSystemLog log;
}
