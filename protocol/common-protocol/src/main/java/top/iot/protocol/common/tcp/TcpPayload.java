package top.iot.protocol.common.tcp;


public interface TcpPayload {

    byte[] toBytes();

    void fromBytes(byte[] bytes,int offset);


}
