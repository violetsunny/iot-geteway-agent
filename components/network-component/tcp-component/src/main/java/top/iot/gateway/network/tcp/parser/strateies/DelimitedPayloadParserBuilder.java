package top.iot.gateway.network.tcp.parser.strateies;

import top.iot.gateway.component.common.ValueObject;
import io.vertx.core.parsetools.RecordParser;
import org.apache.commons.lang.StringEscapeUtils;
import top.iot.gateway.network.tcp.parser.PayloadParserType;

public class DelimitedPayloadParserBuilder extends VertxPayloadParserBuilder {
    @Override
    public PayloadParserType getType() {
        return PayloadParserType.DELIMITED;
    }

    @Override
    protected RecordParser createParser(ValueObject config) {

        return RecordParser.newDelimited(StringEscapeUtils.unescapeJava(config.getString("delimited")
                .orElseThrow(() -> new IllegalArgumentException("delimited can not be null"))));
    }


}
