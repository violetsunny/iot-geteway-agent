/**
 * llkang.com Inc.
 * Copyright (c) 2010-2023 All Rights Reserved.
 */
package top.iot.gateway.component.elasticsearch.configuration;

import lombok.Data;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

/**
 * @author kanglele
 * @version $Id: EsBufferConfig, v 0.1 2023/1/13 11:29 kanglele Exp $
 */
@Data
@Configuration
public class EsBufferConfig {
    //最小间隔
    private int rate = Integer.getInteger("elasticsearch.buffer.rate", 1000);
    //缓冲最大数量
    private int bufferSize = Integer.getInteger("elasticsearch.buffer.size", 3000);
    //缓冲超时时间
    private Duration bufferTimeout = Duration.ofSeconds(Integer.getInteger("elasticsearch.buffer.timeout", 3));
    //背压堆积数量限制.
    private int bufferBackpressure = Integer.getInteger("elasticsearch.buffer.backpressure", Runtime
            .getRuntime()
            .availableProcessors());
    //最大缓冲字节
    private DataSize bufferBytes = DataSize.parse(System.getProperty("elasticsearch.buffer.bytes", "15MB"));
    //最大重试次数
    private int maxRetry = 3;
    //重试间隔
    private Duration minBackoff = Duration.ofSeconds(3);

    private boolean refreshWhenWrite = false;
}
