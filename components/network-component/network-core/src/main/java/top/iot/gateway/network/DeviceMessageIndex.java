/**
 * llkang.com Inc.
 * Copyright (c) 2010-2022 All Rights Reserved.
 */
package top.iot.gateway.network;

import top.iot.gateway.component.common.utils.DateUtils;
import top.iot.gateway.component.elasticsearch.index.ElasticIndex;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 设备es索引
 *
 * @author kanglele
 * @version $Id: DeviceMessageIndex, v 0.1 2022/8/23 16:01 kanglele Exp $
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceMessageIndex implements ElasticIndex {

    private String logType;

    private long timestamp;

    @Override
    public String getIndex() {
        return "custom_"+logType + "_" + DateUtils.timestampToDayHour(timestamp);
    }
}
