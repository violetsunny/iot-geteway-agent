package top.iot.gateway.network.mq.devtype;

/**
 * @author ruanhong
 */

public enum ResumeType {

    YES("Y"), NO("N");

    public String getFlag() {
        return flag;
    }

    private String flag;

    private ResumeType(String flag) {
        this.flag = flag;
    }


}
