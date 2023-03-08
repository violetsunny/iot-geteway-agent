/**
 * llkang.com Inc.
 * Copyright (c) 2010-2022 All Rights Reserved.
 */
package top.iot.gateway.network.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.annotation.Resource;

/**
 * kafka生产者
 *
 * @author kanglele
 * @version $Id: KafkaProducer, v 0.1 2022/8/19 17:29 kanglele Exp $
 */
@Component
@Slf4j
public class KafkaProducer {

    @Resource
    private KafkaTemplate<String, Object> kafkaTemplate;

    public boolean send(String topic, Object message) {
        try {
            // 异步获取发送结果
            ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, message);
            future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
                @Override
                public void onFailure(Throwable throwable) {
                    log.error("{} - 生产者 发送消息失败：", topic, throwable);
                }
                @Override
                public void onSuccess(SendResult<String, Object> stringObjectSendResult) {
                    log.info("{} - 生产者 发送消息成功：{}", topic, stringObjectSendResult.toString());
                }
            });
        } catch (Exception e) {
            log.error("{} - 生产者 发送消息异常：", topic, e);
            return false;
        }
        return true;
    }

}
