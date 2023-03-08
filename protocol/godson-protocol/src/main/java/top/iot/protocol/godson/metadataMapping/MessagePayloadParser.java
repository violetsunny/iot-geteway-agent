package top.iot.protocol.godson.metadataMapping;

import org.hswebframework.web.bean.FastBeanCopier;
import top.iot.gateway.core.message.CommonDeviceMessage;
import top.iot.gateway.core.message.Message;
import top.iot.gateway.core.message.codec.EncodedMessage;
import top.iot.gateway.core.message.codec.MessagePayloadType;
import top.iot.gateway.core.message.event.EventMessage;
import top.iot.gateway.core.message.property.ReportPropertyMessage;
import top.iot.gateway.core.metadata.DeviceMetadataType;
import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface MessagePayloadParser {
    MessagePayloadType getType();
    default Publisher<? extends Message> handle(String deviceId, Map<String, Object> configuration, EncodedMessage message) {
        return null;
    };

    Publisher<? extends Message> handle(String deviceId, DeviceMetadataMapping metadataMapping, EncodedMessage message);

    default Message createReportMessage(CommonDeviceMessage deviceMessage, DeviceMetadataMapping metadataMapping, Object payload) {
        List<MetadataMapping> properties = metadataMapping.getProperties();
        Map<String, Object> metaMap = parseExpressions(properties, payload, DeviceMetadataType.property);
        if (metaMap.size() > 0) {
            ReportPropertyMessage propertyMessage = new ReportPropertyMessage();
            FastBeanCopier.copy(deviceMessage, propertyMessage);
            propertyMessage.setProperties(metaMap);
            return propertyMessage;
        }
        return null;
    }

    default List<Message> createEventMessages(CommonDeviceMessage deviceMessage, DeviceMetadataMapping metadataMapping, Object payload) {
        List<Message> eventMessages = new ArrayList<>();
        parseExpressions(metadataMapping.getEvents(), payload, DeviceMetadataType.event).forEach((k,v) -> {
            EventMessage eventMessage = new EventMessage();
            FastBeanCopier.copy(deviceMessage, eventMessage);
            eventMessage.setEvent(k);
            eventMessage.setData(v);

            eventMessages.add(eventMessage);
        });
        return eventMessages;
    }

    Map<String, Object> parseExpressions(List<? extends MetadataMapping> metadataMappings, Object payload, DeviceMetadataType type);

    Object parseScriptExpression(Map<String, Object> expression, Object value);

    Map<String, Object> parseParamExpressions(List<? extends MetadataMapping> metadataMappings, String payload, DeviceMetadataType type);
}
