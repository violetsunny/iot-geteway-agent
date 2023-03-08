/**
 * llkang.com Inc.
 * Copyright (c) 2010-2022 All Rights Reserved.
 */
package top.iot.gateway.network.mq.devtype;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * iot设备类型消息
 *
 * @author kanglele
 * @version $Id: IotDevTypeMessage, v 0.1 2022/8/19 17:26 kanglele Exp $
 */
@NoArgsConstructor
@Data
public class IotDevTypeMessage {

    @JsonProperty("version")
    private String version;
    @JsonProperty("devId")
    private String devId;
    @JsonProperty("devType")
    private String devType;
    @JsonProperty("productId")
    private String productId;
    @JsonProperty("tenantId")
    private String tenantId;
    @JsonProperty("ts")
    private Long ts;
    /**
     *  0-否，1-是
     */
    @JsonProperty("debug")
    private Integer debug;
    /**
     * N-否，Y-是
     */
    @JsonProperty("resume")
    private String resume;
    /**
     * 2688/cim/custom
     */
    @JsonProperty("source")
    private String source;
    @JsonProperty("deviceName")
    private String deviceName;
    @JsonProperty("period")
    private String period;
    @JsonProperty("sn")
    private String sn;
    @JsonProperty("data")
    private List<Metrics> data;

    @NoArgsConstructor
    @Data
    public static class Metrics {
        @JsonProperty("metric")
        private String metric;
        @JsonProperty("value")
        private Object value;
    }
}
