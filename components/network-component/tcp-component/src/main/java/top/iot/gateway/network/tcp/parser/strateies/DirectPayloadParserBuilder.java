package top.iot.gateway.network.tcp.parser.strateies;

import top.iot.gateway.component.common.ValueObject;
import top.iot.gateway.network.tcp.parser.PayloadParser;
import top.iot.gateway.network.tcp.parser.PayloadParserBuilderStrategy;
import top.iot.gateway.network.tcp.parser.DirectRecordParser;
import lombok.SneakyThrows;
import top.iot.gateway.network.tcp.parser.PayloadParserType;

public class DirectPayloadParserBuilder implements PayloadParserBuilderStrategy {

    @Override
    public PayloadParserType getType() {
        return PayloadParserType.DIRECT;
    }

    @Override
    @SneakyThrows
    public PayloadParser build(ValueObject config) {
        return new DirectRecordParser();
    }
}
