package top.iot.gateway.network.tcp.parser.strateies;

import top.iot.gateway.component.common.ValueObject;
import top.iot.gateway.network.tcp.parser.PayloadParser;
import top.iot.gateway.network.tcp.parser.PayloadParserBuilderStrategy;
import top.iot.gateway.network.tcp.parser.PayloadParserType;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.expands.script.engine.DynamicScriptEngine;
import org.hswebframework.expands.script.engine.DynamicScriptEngineFactory;

import java.util.HashMap;
import java.util.Map;

public class ScriptPayloadParserBuilder implements PayloadParserBuilderStrategy {
    @Override
    public PayloadParserType getType() {
        return PayloadParserType.SCRIPT;
    }

    @Override
    @SneakyThrows
    public PayloadParser build(ValueObject config) {
        String script = config.getString("script")
            .orElseThrow(() -> new IllegalArgumentException("script不能为空"));
        String lang = config.getString("lang")
            .orElseThrow(() -> new IllegalArgumentException("lang不能为空"));

        DynamicScriptEngine engine = DynamicScriptEngineFactory.getEngine(lang);
        if (engine == null) {
            throw new IllegalArgumentException("不支持的脚本:" + lang);
        }
        PipePayloadParser parser = new PipePayloadParser();
        String id = DigestUtils.md5Hex(script);
        if (!engine.compiled(id)) {
            engine.compile(id, script);
        }
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("parser", parser);
        engine.execute(id, ctx).getIfSuccess();
        return parser;
    }
}
