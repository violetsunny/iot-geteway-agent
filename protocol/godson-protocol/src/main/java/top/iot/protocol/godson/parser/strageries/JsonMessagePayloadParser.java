package top.iot.protocol.godson.parser.strageries;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import top.iot.protocol.godson.metadataMapping.DeviceMetadataMapping;
import top.iot.protocol.godson.metadataMapping.MessagePayloadParserBuilder;
import top.iot.protocol.godson.metadataMapping.MetadataMapping;
import top.iot.protocol.godson.utils.ToBinBitUtils;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.utils.time.DateFormatter;
import top.iot.gateway.core.message.CommonDeviceMessage;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.codec.EncodedMessage;
import top.iot.gateway.core.message.codec.MessagePayloadType;
import top.iot.gateway.core.metadata.DeviceMetadataType;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
@Slf4j
public class JsonMessagePayloadParser extends CommonMessagePayloadParser {

    @Autowired
    private MessagePayloadParserBuilder messagePayloadParserBuilder;

    @Override
    public MessagePayloadType getType() {
        return MessagePayloadType.JSON;
    }

    @Override
    public Publisher<? extends Message> handle(String deviceId, DeviceMetadataMapping metadataMapping, EncodedMessage message) {
        log.info("json解析器解码设备{}消息", deviceId);
        String payload = message.getPayload().toString(StandardCharsets.UTF_8);
        if (ToBinBitUtils.isBase64(payload)) {
            payload = new String(Base64.getDecoder().decode(payload));
        }
        JSONObject jsonObject = JSON.parseObject(payload);

        List<Message> messages = new ArrayList<>();


        CommonDeviceMessage deviceMessage = new CommonDeviceMessage();
        deviceMessage.setDeviceId(deviceId);
        deviceMessage.setTimestamp(jsonObject.getLong("timestamp")==null ? System.currentTimeMillis() : jsonObject.getLong("timestamp"));
        deviceMessage.setMessageId(jsonObject.getString("eventId")==null ? UUID.randomUUID().toString(): jsonObject.getString("eventId"));

        String properties = jsonObject.getString("properties");
        log.info("json解析器解析设备{}的properties", deviceId);

        handleMessage(messages, deviceMessage, metadataMapping, payload, properties);
        log.info("json解析器解析设备{}的events", deviceId);
        String events = jsonObject.getString("events");
        handleMessage(messages, deviceMessage, metadataMapping, payload, events);

        log.info("json解析器解析设备{}后的消息为：{}" , deviceId, messages);
        return Flux.fromIterable(messages);
    }

    private void handleMessage(List<Message> messages,
                               CommonDeviceMessage deviceMessage,
                               DeviceMetadataMapping metadataMapping,
                               String payload,
                               String body) {
        if (StringUtils.hasText(body)) {
            if (ToBinBitUtils.isBase64(body)) {
                body = new String(Base64.getDecoder().decode(body));
            }

            if (body.startsWith("{")) {
                log.info("json解析器开始解析单条消息，内容为:{}", payload);
                log.info("json解析器开始解析单条消息中的事件");
                List<Message> eventMessages = createEventMessages(deviceMessage, metadataMapping, payload);
                if (eventMessages.size() > 0) {
                    messages.addAll(eventMessages);
                }
                log.info("json解析器开始解析单条消息中的事件,解析后的消息:{}", messages);
                log.info("json解析器开始解析单条消息中的属性");
                Message reportMessage = createReportMessage(deviceMessage, metadataMapping, payload);
                if (reportMessage != null) {
                    messages.add(reportMessage);
                }
                log.info("json解析器开始解析单条消息中的属性,解析后的消息:{}", messages);
            } else if (body.startsWith("[")) {
                for (int i = 0; i< JSON.parseArray(body).size(); i++) {
                    List<Message> eventMessages = createEventMessages(deviceMessage, metadataMapping, payload);
                    if (eventMessages.size() > 0) {
                        messages.addAll(eventMessages);
                    }
                    Message reportMessage = createReportMessage(deviceMessage, metadataMapping, payload);
                    if (reportMessage != null) {
                        messages.add(reportMessage);
                    }
                }
            } else {
                List<Message> eventMessages = messagePayloadParserBuilder.build(MessagePayloadType.STRING).createEventMessages(deviceMessage, metadataMapping, body.getBytes());
                if (eventMessages.size() > 0) {
                    messages.addAll(eventMessages);
                }
                Message reportMessage = messagePayloadParserBuilder.build(MessagePayloadType.STRING).createReportMessage(deviceMessage, metadataMapping, body.getBytes());
                if (reportMessage != null) {
                    messages.add(reportMessage);
                }
            }
        }
    }

    @Override
    public Map<String, Object> parseExpressions(List<? extends MetadataMapping> metadataMappings, Object body, DeviceMetadataType type) {
        String payload = String.valueOf(body);
        Map<String, Object> metaMap = new HashMap<>();

        metadataMappings.forEach(metadataMapping -> {
            if (CollectionUtils.isEmpty(metadataMapping.getReports())) {
                return;
            }
            metadataMapping.getReports()
                .stream()
                .filter(report -> !report.trim().isEmpty() && JSONPath.contains(payload, report))
                .findFirst().ifPresent(reportName -> {
                Object value = JSONPath.read(payload, reportName);
                log.info("json解析器解析上报属性/事件{},值为{}", reportName, value);
                if (value != null) {
                    if (!metadataMapping.isComplex()) {
                        if ("date".equalsIgnoreCase(metadataMapping.getType())) {
                            log.warn("日期类型的字段:" + metadataMapping.getName() + ",值::" + value);
                            if (!((String)value).matches("\\d+")) {
                                value = DateFormatter.fromString((String) value).getTime();
                            }
                        }
                        if (!StringUtils.isEmpty(metadataMapping.getExpression("script").orElse(null))) {
                            //执行表达式解析流程
                            value = parseScriptExpression(metadataMapping.getExpression(), value);
                        }
                        Object filterExpression = metadataMapping.getExpand("filterExpression").orElse(null);
                        if (StringUtils.isEmpty(filterExpression) || StringUtils.isEmpty(JSON.parseObject(String.valueOf(filterExpression)).getString("script"))) {
                            if (type!=DeviceMetadataType.event || !"object".equals(metadataMapping.getType())) {
                                metaMap.put(metadataMapping.getId(), value);
                            }
                        } else {
                            Object obj = parseScriptExpression(JSON.parseObject(String.valueOf(metadataMapping.getExpand("filterExpression").orElse(null))), value);
                            if (type==DeviceMetadataType.property) {
                                metaMap.put(metadataMapping.getId(), value);
                            } else if (type==DeviceMetadataType.event && obj instanceof ScriptObjectMirror) {
                                metaMap.put(metadataMapping.getId(), obj);
                            }
                        }
                    } else {
                        if (value.toString().startsWith("[") || value.toString().startsWith("{")) {
                            Map<String, Object> paramMap = parseExpressions(metadataMapping.getParameters(), payload, type);
                            if (paramMap.size() > 0) {
                                metaMap.put(metadataMapping.getId(), paramMap);
                            }
                        } else {
                            if (!StringUtils.isEmpty(metadataMapping.getExpression("script").orElse(null))) {
                                //执行表达式解析流程
                                value = parseScriptExpression(metadataMapping.getExpression(), value);
                            }
                            Map<String, Object> paramMap = parseParamExpressions(metadataMapping.getParameters(), value.toString(), type);
                            if (paramMap.size() > 0) {
                                metaMap.put(metadataMapping.getId(), paramMap);
                            }
                        }
                    }
                }
            });
        });

        return metaMap;
    }

}