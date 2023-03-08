package top.iot.protocol.common;

import com.alibaba.fastjson.JSONObject;
import top.iot.gateway.core.message.ChildDeviceMessage;
import top.iot.gateway.core.message.DeviceMessage;
import top.iot.gateway.core.message.event.EventMessage;
import top.iot.protocol.common.mqtt.MqttDeviceMessageCodec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TopicMessageCodecTest {

    @Test
    void testChildrenMessage() {
        TopicMessageCodec codec = new MqttDeviceMessageCodec();

        DeviceMessage message = codec.doDecode("test", "/children/fire_alarm", new JSONObject());

        assertTrue(message instanceof ChildDeviceMessage);
        ChildDeviceMessage msg = ((ChildDeviceMessage) message);
        assertTrue(msg.getChildDeviceMessage() instanceof EventMessage);

    }

}