/**
 * llkang.com Inc.
 * Copyright (c) 2010-2022 All Rights Reserved.
 */
package top.iot.gateway.network;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 设备缓存
 *
 * @author kanglele
 * @version $Id: DeviceCacheModel, v 0.1 2022/9/15 17:49 kanglele Exp $
 */
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@Data
public class DeviceCacheModel implements Serializable {

    private String deviceId;

    private String index;

    private List<String> keys;

    private Map<String,Object> values;

}
