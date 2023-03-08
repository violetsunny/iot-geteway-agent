package top.iot.protocol.common.tcp.message;

import top.iot.protocol.common.tcp.TcpStatus;
import org.apache.commons.codec.binary.Hex;
import org.hswebframework.web.id.IDGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthResponseTest {

    @Test
    void test() {
        AuthResponse response = AuthResponse.of(IDGenerator.SNOW_FLAKE.generate(), TcpStatus.SUCCESS);

        byte[] encode = response.toBytes();
        System.out.println(Hex.encodeHexString(encode));
        System.out.println(response);

        AuthResponse decode = AuthResponse.of(encode, 0);

        assertEquals(response.getDeviceId(), decode.getDeviceId());
        assertEquals(response.getStatus(), decode.getStatus());


    }

}