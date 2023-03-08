package top.iot.gateway.network.tcp.parser;

import top.iot.gateway.component.common.ValueObject;

public interface PayloadParserBuilderStrategy {
    PayloadParserType getType();

    PayloadParser build(ValueObject config);
}
