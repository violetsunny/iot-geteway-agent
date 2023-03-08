package top.iot.protocol.common.udp;

import top.iot.gateway.core.message.codec.EncodedMessage;

/**
 * @author wangzheng
 * @see
 * @since 1.0
 */

public interface DemoUdpMessage extends EncodedMessage {

    String getType();

    String getDeviceId();


}
