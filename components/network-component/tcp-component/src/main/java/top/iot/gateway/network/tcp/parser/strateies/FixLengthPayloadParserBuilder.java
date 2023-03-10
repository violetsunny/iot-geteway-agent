package top.iot.gateway.network.tcp.parser.strateies;

import top.iot.gateway.component.common.ValueObject;
import io.vertx.core.parsetools.RecordParser;
import top.iot.gateway.network.tcp.parser.PayloadParserType;

public class FixLengthPayloadParserBuilder extends VertxPayloadParserBuilder {
    @Override
    public PayloadParserType getType() {
        return PayloadParserType.FIXED_LENGTH;
    }

    @Override
    protected RecordParser createParser(ValueObject config) {
        return RecordParser.newFixed(config.getInt("size")
                .orElseThrow(() -> new IllegalArgumentException("size can not be null")));
    }


}
