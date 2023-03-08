package top.iot.gateway.manager.message;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
//@Component
public class RabbitPropertyListener {

//    @RabbitHandler
////    @RabbitListener(queues = "#{propertyQueue.name}")
//    @RabbitListener(bindings = {
//         @QueueBinding(
//                 exchange = @Exchange(value = "iot.exchange.property", durable = "true", type = "topic"),
//                 value = @Queue(value = "property_consumer_queue",durable = "true"),
//                 key = "device.#.property.*"
//         )
//    })
//    public void devicePropertyReceive(Channel channel, Message message, JSONObject jsonObject) {
//        try {
//            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
//            log.debug("接收到的属性消息:{}", jsonObject);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    @RabbitHandler
//    @RabbitListener(bindings = {
//            @QueueBinding(
//                    exchange = @Exchange(value = "iot.exchange.event", durable = "true", type = "topic"),
//                    value = @Queue(value = "event_consumer_queue",durable = "true"),
//                    key = {"device.#.event.*", "device.#.online", "device.#.offline"}
//            )
//    })
//    public void deviceEventReceive(Channel channel, Message message, JSONObject jsonObject) {
//        try {
//            channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
//            log.debug("接收到的事件消息:{}", jsonObject);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}

