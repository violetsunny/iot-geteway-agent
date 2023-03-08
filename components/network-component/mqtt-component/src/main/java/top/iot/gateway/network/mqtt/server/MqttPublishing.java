package top.iot.gateway.network.mqtt.server;

import top.iot.gateway.core.message.codec.MqttMessage;
import top.iot.gateway.core.server.mqtt.MqttPublishingMessage;

public interface MqttPublishing extends MqttPublishingMessage {

    MqttMessage getMessage();

    void acknowledge();
}
