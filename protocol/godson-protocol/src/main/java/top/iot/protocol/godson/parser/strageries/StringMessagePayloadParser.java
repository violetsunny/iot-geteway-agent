package top.iot.protocol.godson.parser.strageries;

import com.alibaba.fastjson.JSON;
import top.iot.protocol.godson.metadataMapping.DeviceMetadataMapping;
import top.iot.protocol.godson.metadataMapping.MetadataMapping;
import top.iot.protocol.godson.utils.ToBinBitUtils;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.hswebframework.web.bean.FastBeanCopier;
import top.iot.gateway.core.message.CommonDeviceMessage;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.codec.EncodedMessage;
import top.iot.gateway.core.message.codec.MessagePayloadType;
import top.iot.gateway.core.message.event.EventMessage;
import top.iot.gateway.core.message.property.ReportPropertyMessage;
import top.iot.gateway.core.metadata.DeviceMetadataType;
import top.iot.gateway.core.utils.BytesUtils;
import org.reactivestreams.Publisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Component
@Slf4j
public class StringMessagePayloadParser extends CommonMessagePayloadParser {

    @Override
    public MessagePayloadType getType() {
        return MessagePayloadType.STRING;
    }

    protected List<Message> handle(String deviceId, DeviceMetadataMapping metadataMapping, Object payload) {
        log.info("string解析器解码设备{}消息", deviceId);
        List<Message> messages = new ArrayList<>();

        //设置消息时间：以当前时间戳为消息时间
        long time = LocalDateTime.now().toInstant(ZoneOffset.of("+8")).toEpochMilli();

        CommonDeviceMessage deviceMessage = new CommonDeviceMessage();
        deviceMessage.setDeviceId(deviceId);
        deviceMessage.setTimestamp(time);

        if (!StringUtils.isEmpty(metadataMapping.getMsgInfo("hasProperties").orElse(null))) {
            if (!StringUtils.isEmpty(metadataMapping.getMsgInfo("isMulti").orElse(null))) {
                int msgNum = (int) metadataMapping.getMsgInfo("msgNum").orElseThrow(() -> new RuntimeException("消息总数没设置"));
                Flux.range(1, msgNum).doOnNext(num -> {
                    int step = (int) metadataMapping.getMsgInfo("propStep").orElseThrow(() -> new RuntimeException("属性步长没设置"));
                    ReportPropertyMessage propertyMessage = new ReportPropertyMessage();
                    FastBeanCopier.copy(deviceMessage, propertyMessage);
                    propertyMessage.setProperties(parseExpressions(metadataMapping.getProperties(), payload, step * (num - 1), DeviceMetadataType.property));
                    messages.add(propertyMessage);
                });
            } else {
                messages.add(createReportMessage(deviceMessage, metadataMapping, payload));
            }
        }
        if (!StringUtils.isEmpty(metadataMapping.getMsgInfo("hasEvents").orElse(null))) {
            if (!StringUtils.isEmpty(metadataMapping.getMsgInfo("isMulti").orElse(null))) {
                int msgNum = (int) metadataMapping.getMsgInfo("msgNum").orElseThrow(() -> new RuntimeException("消息总数没设置"));
                Flux.range(1, msgNum).doOnNext(num -> {
                    int step = (int) metadataMapping.getMsgInfo("eventStep").orElseThrow(() -> new RuntimeException("事件步长没设置"));
                    List<Message> eventMessages = new ArrayList<>();
                    parseExpressions(metadataMapping.getEvents(), payload, step * (num - 1), DeviceMetadataType.event).forEach((k, v) -> {
                        EventMessage eventMessage = new EventMessage();
                        FastBeanCopier.copy(deviceMessage, eventMessage);
                        eventMessage.setEvent(k);
                        eventMessage.setData(v);

                        eventMessages.add(eventMessage);
                    });
                    messages.addAll(eventMessages);
                });
            } else {
                messages.addAll(createEventMessages(deviceMessage, metadataMapping, payload));
            }
        }
        return messages;
    }

    @Override
    public Publisher<? extends Message> handle(String deviceId, DeviceMetadataMapping metadataMapping, EncodedMessage message) {
        String payload = message.payloadAsString();
        if (ToBinBitUtils.isBase64(payload)) {
            payload = new String(Base64.getDecoder().decode(payload));
        }

        List<Message> messages = handle(deviceId, metadataMapping, payload.getBytes());

        return Flux.fromIterable(messages);
    }

    @Override
    public Map<String, Object> parseExpressions(List<? extends MetadataMapping> metadataMappings, Object payload, DeviceMetadataType type) {
        return parseExpressions(metadataMappings, payload, 0, type);
    }

    private Map<String, Object> parseExpressions(List<? extends MetadataMapping> metadataMappings, Object body, int step, DeviceMetadataType deviceMetadataType) {
        byte[] payload = (byte[])body;
        Map<String, Object> metaMap = new HashMap<>();
        metadataMappings.forEach(metadataMapping -> {
            if (metadataMapping.getReports().size() == 0) return;
            String reportName = metadataMapping.getReports().stream().findFirst()
                .orElseThrow(() -> new RuntimeException("不存在" + metadataMapping.getId() + "上报字段"));
            if (reportName.split(",").length != 2) throw new RuntimeException(metadataMapping.getId() + "上报字段格式不对");

            String[] typeArr = {"enum", "float"};
            String type = (metadataMapping.isComplex() || Arrays.asList(typeArr).contains(metadataMapping.getType())) ? "int" : metadataMapping.getType();
            if ("date".equalsIgnoreCase(type)) {
                type = "long";
            }
            Object value = null;
            int offset = Integer.parseInt(reportName.split(",")[0]);
            if (step != 0 && StringUtils.isEmpty(metadataMapping.getExpand("isFixed").orElse(null))) {
                offset += step;
            }
            if (Integer.parseInt(reportName.split(",")[1]) == 0) {
                value = payload;
            } else {
//                String msg = byteToHex(payload);
                String msg = Hex.encodeHexString(payload);
//                if (msg.toLowerCase().startsWith("8a8a")) return;
//                if (msg.toLowerCase().startsWith("7e7e") && msg.length()>=130) {
                if (!StringUtils.isEmpty(metadataMapping.getExpand("isBcd").orElse(null))) {
                    value = msg.substring(2 * offset, 2 * (offset + Integer.parseInt(reportName.split(",")[1])));
                    if (String.valueOf(value).startsWith("ff")) {
                        String str = String.valueOf(value).substring(2);
                        value = "-" + str.replaceFirst("^0*", "");
                    }
                } else {
                    try {
//                    value = BytesUtils.class.getMethod("beTo" + type.substring(0, 1).toUpperCase() + type.substring(1), byte[].class, int.class, int.class)
//                        .invoke(null, payload, offset, Integer.parseInt(reportName.split(",")[1]));
                        log.info("StringMessagePayloadParser开始解析字段" + metadataMapping.getId() + ",上报属性" + metadataMapping.getReports().get(0));
                        value = BytesUtils.beToInt(payload, offset, Integer.parseInt(reportName.split(",")[1]));
                    } catch (Exception e) {
                        log.error("字段" + metadataMapping.getId() + ",上报属性" + metadataMapping.getReports().get(0));
//                    e.printStackTrace();
//                    throw new RuntimeException("字段" + metadataMapping.getId() + ",上报属性" + metadataMapping.getReports().get(0) + "执行beTo" + type.substring(0, 1).toUpperCase() + type.substring(1) + "方法异常");
                    }
                }


            }

            if (!StringUtils.isEmpty(metadataMapping.getExpand("hexFloat").orElse(null))) {
                value = Float.intBitsToFloat((int)value);
            }

            if (value != null) {
                if (!metadataMapping.isComplex()) {
                    if (!StringUtils.isEmpty(metadataMapping.getExpression("script").orElse(null))) {
                        //执行表达式解析流程
                        value = parseScriptExpression(metadataMapping.getExpression(), value);
                    }
                    Object filterExpression = metadataMapping.getExpand("filterExpression").orElse(null);
                    if (StringUtils.isEmpty(filterExpression) || StringUtils.isEmpty(JSON.parseObject(String.valueOf(filterExpression)).getString("script"))) {
                        if (deviceMetadataType != DeviceMetadataType.event || !"object".equals(metadataMapping.getType())) {
                            metaMap.put(metadataMapping.getId(), value);
                        }
                    } else {
                        Object obj = parseScriptExpression(JSON.parseObject(String.valueOf(metadataMapping.getExpand("filterExpression").orElse(null))), value);
                        if (deviceMetadataType == DeviceMetadataType.property) {
                            metaMap.put(metadataMapping.getId(), value);
                        } else if (deviceMetadataType == DeviceMetadataType.event && obj instanceof ScriptObjectMirror) {
                            metaMap.put(metadataMapping.getId(), obj);
                        }
                    }
                } else {
                    if (!StringUtils.isEmpty(metadataMapping.getExpression("script").orElse(null))) {
                        //执行表达式解析流程
                        value = parseScriptExpression(metadataMapping.getExpression(), value);
//                            log.info("执行表达式:{},解析结果为:{}", metadataMapping.getExpression("script"), value);
                    }
                    Map<String, Object> paramMap = parseParamExpressions(metadataMapping.getParameters(), value.toString(), deviceMetadataType);
                    if (paramMap.size() > 0) {
                        metaMap.put(metadataMapping.getId(), paramMap);
                    }
                }
            }

        });
        return metaMap;
    }
}
