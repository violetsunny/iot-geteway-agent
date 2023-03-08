package top.iot.gateway.network.tcp.parser;

import top.iot.gateway.component.common.ValueObject;

public interface PayloadParserBuilder {

    PayloadParser build(PayloadParserType type, ValueObject configuration);

}
