package top.iot.gateway.logging.event.handler;

import top.iot.gateway.component.elasticsearch.index.ElasticIndex;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LoggerIndexProvider implements ElasticIndex {

    ACCESS("access_logger"),
    SYSTEM("system_logger");

    private String index;
}
