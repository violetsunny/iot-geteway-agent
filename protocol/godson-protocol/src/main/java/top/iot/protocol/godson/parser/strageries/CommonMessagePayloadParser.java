package top.iot.protocol.godson.parser.strageries;

import com.alibaba.fastjson.JSON;
import top.iot.protocol.godson.metadataMapping.MessagePayloadParser;
import top.iot.protocol.godson.metadataMapping.MetadataMapping;
import top.iot.protocol.godson.utils.ToBinBitUtils;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.hswebframework.expands.script.engine.DynamicScriptEngine;
import org.hswebframework.expands.script.engine.DynamicScriptEngineFactory;
import top.iot.gateway.core.metadata.DeviceMetadataType;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class CommonMessagePayloadParser implements MessagePayloadParser {

//            Map<String, Object> config = new HashMap<>();
//    config.put("script", "\n" +
//        "var ToBinBitUtils = top.iot.gateway.community.network.tcp.parser.strateies.ToBinBitUtils;\n" +
//        "parser.fixed(currentValue.length)\n" +
//        "       .handler(function(buffer){\n" +
//        "            var str = buffer.toString(\"UTF-8\");\n" +
//        "            var value = parseInt(str.split(\",\")[1]);\n" +
//        "            var bits=8,startBit=4,endBit=7;\n" +
//        "            var i = ToBinBitUtils.toInt(ToBinBitUtils.toBit(value, bits), startBit, endBit);" +
//        "            parser.result(i).complete();\n" +
//        "});");
//    config.put("lang", "javascript");

//    String deviceStatus = "var BytesUtils = top.iot.gateway.core.utils.BytesUtils;\n" +
//        "parser.fixed(currentValue.length)\n" +
//        "   .handler(function(buffer){\n" +
//        "       var value=buffer.toString(\"UTF-8\");\n" +
//        "       var offset=3,byteLen=1;\n" +
//        "       var result=BytesUtils.beToInt(value, offset, byteLen);\n" +
//        "       parser.result(result).complete();";
//    String deviceStatus_sensor = "var ToBinBitUtils = top.iot.gateway.community.network.tcp.parser.strateies.ToBinBitUtils;\n" +
//        "parser.fixed(currentValue.length)\n" +
//    "       .handler(function(buffer){\n" +
//    "           var value=buffer.toString(\"UTF-8\");\n" +
//    "           var bitLen=8,startBit=6,endBit=7;\n" +
//    "           var result=ToBinBitUtils.toInt(ToBinBitUtils.toBit(value, bitLen), startBit, endBit);\n" +
//    "           parser.result(result).complete();";


//    var ToBinBitUtils = org.godsonIot.core.protocol.utils.ToBinBitUtils;
//    var BytesUtils = top.iot.gateway.community.network.utils.BytesUtils;
//    var bitLen=8,startBit=4,endBit=7;return ToBinBitUtils.toInt(ToBinBitUtils.toBit(currentValue, bitLen), startBit, endBit);
//    var str = "8,1,8,4,7_8,1,8,0,3_9,1,8,4,7_9,1,8,0,3_10,1,8,4,7_10,1,8,0,3";
//    var split = decode.split("_");
//    var valueArr = new Array(split.length);
//for(i=0;i<split.length;i++){
//        var $offset = parseInt(split[i].split(",")[0]);
//        var $len = parseInt(split[i].split(",")[1]);
//        var $bitLen = parseInt(split[i].split(",")[2]);
//        var $start = parseInt(split[i].split(",")[3]);
//        var $end = parseInt(split[i].split(",")[4]);
//        valueArr[i] = ToBinBitUtils.toInt(ToBinBitUtils.toBit(BytesUtils.beToInt(payload, $offset, $len), $bitLen), $start, $end);
//    }
//    var value = "";
//switch (valueArr[0]) {
//        case 1:
//            value = valueArr[4] + "." + valueArr[5];
//            break;
//        case 2:
//            value = valueArr[3] + "." + valueArr[4] + "" + valueArr[5];
//            break;
//        case 3:
//            value = valueArr[2] + "." + valueArr[3] + "" + valueArr[4] + "" + valueArr[5];
//            break;
//        case 4:
//            value = valueArr[1] + "." + valueArr[2] + "" + valueArr[3] + "" + valueArr[4] + "" + valueArr[5];
//            break;
//    }
//return value;

    //       String script = "return parseFloat(currentValue)+10";  //简单算术
//       String script = "return (currentValue >> (8-1)) & 1"; //每一位  比如,第8位
//       String script = "return (currentValue & 0xf0) >> 4"; //高4位
//    String script = "return currentValue & 0x0f";    //低4位

    @Override
    @SneakyThrows
    public Object parseScriptExpression(Map<String, Object> expression, Object value) {
        String lang = (String)expression.get("lang");
        String script = (String)expression.get("script");
        script = script.replaceAll("\\\\", "");

        DynamicScriptEngine engine = DynamicScriptEngineFactory.getEngine(lang);
        if (engine == null) {
            throw new IllegalArgumentException("不支持的脚本:" + lang);
        }
        String id = DigestUtils.md5Hex(script);
        if (!engine.compiled(id)) {
            engine.compile(id, script);
        }
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("$currentValue", value);
        ctx.put("$illegalArgs", "illegalArgs");
        Object result = engine.execute(id, ctx).getIfSuccess();
        if ("illegalArgs".equals(result)) throw new IllegalArgumentException("不合法的参数值:" + value);
        return result;
    }

    @Override
    public Map<String, Object> parseParamExpressions(List<? extends MetadataMapping> metadataMappings, String payload, DeviceMetadataType type) {
        Map<String, Object> paramMap = new HashMap<>();

        metadataMappings.forEach(metadataMapping -> {
            if (metadataMapping.getReports().size() == 0) return;
            String paramName = metadataMapping.getReports().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("不存在" + metadataMapping.getId() + "上报字段"));
            String[] paramArr = paramName.split(",");
            Object paramValue = ToBinBitUtils.toInt(ToBinBitUtils.toBit(Integer.parseInt(payload), Integer.parseInt(paramArr[0])), Integer.parseInt(paramArr[1]), Integer.parseInt(paramArr[2]));
            if (!paramValue.equals(-1)) {
                if (!metadataMapping.isComplex()) {
                    if (!StringUtils.isEmpty(metadataMapping.getExpression("script").orElse(null))) {
                        //执行表达式解析流程
                        paramValue = parseScriptExpression(metadataMapping.getExpression(), paramValue);
                    }
                    Object filterExpression = metadataMapping.getExpand("filterExpression").orElse(null);
                    if (StringUtils.isEmpty(filterExpression) || StringUtils.isEmpty(JSON.parseObject(String.valueOf(filterExpression)).getString("script"))) {
                        if (type!=DeviceMetadataType.event) {
                            paramMap.put(metadataMapping.getId(), paramValue);
                        }
                    } else {
                        Object obj = parseScriptExpression(JSON.parseObject(String.valueOf(metadataMapping.getExpand("filterExpression").orElse(null))), paramValue);
                        if (type==DeviceMetadataType.property) {
                            paramMap.put(metadataMapping.getId(), paramValue);
                        } else if (type==DeviceMetadataType.event && obj instanceof ScriptObjectMirror) {
                            paramMap.put(metadataMapping.getId(), obj);
                        }
                    }
                } else {
                    if (!StringUtils.isEmpty(metadataMapping.getExpression("script").orElse(null))) {
                        //执行表达式解析流程
                        paramValue = parseScriptExpression(metadataMapping.getExpression(), paramValue);
                    }
                    Map<String, Object> map = parseParamExpressions(metadataMapping.getParameters(), paramValue.toString(), type);
                    if (map.size() > 0) {
                        paramMap.put(metadataMapping.getId(), map);
                    }
                }
            }
        });
        return paramMap;
    }
}
