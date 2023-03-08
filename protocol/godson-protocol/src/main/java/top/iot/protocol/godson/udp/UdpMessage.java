package top.iot.protocol.godson.udp;

import top.iot.gateway.core.message.codec.EncodedMessage;

/**
 * @author wangzheng
 * @see
 * @since 1.0
 */

public interface UdpMessage extends EncodedMessage {

    String getType();

    String getDeviceId();


}
