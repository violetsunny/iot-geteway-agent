package top.iot.gateway.agent.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Slf4j
//@Configuration
public class RabbitConfiguration {
//    @Bean
//    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
//        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
//        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
//        rabbitTemplate.setMandatory(true);
//        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
//            log.error("Send message({}) to exchange({}), routingKey({}) failed: {}", message, exchange, routingKey, replyText);
//        });
//        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
//            if (!ack) {
//                log.error("CorrelationData({}) ack failed: {}", correlationData, cause);
//            }
//        });
//        return rabbitTemplate;
//    }

//    @Bean
//    public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
//        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
//        factory.setConnectionFactory(connectionFactory);
//        factory.setMessageConverter(new Jackson2JsonMessageConverter());
//        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
//        return factory;
//    }

//    @Bean
//    TopicExchange propertyExchange() {
//        return new TopicExchange("iot.exchange.property", true, false);
//    }
//
//    @Bean
//    Queue propertyQueue() {
//        Map<String, Object> arguments = new HashMap<>();
//        // 30天： 30 * 24 * 60 * 60 * 1000 = 2592000000L
//        arguments.put("x-message-ttl", 15000L);
//        return new Queue("iot.queue.property", true, false, false, arguments);
//    }
//
//    @Bean
//    Binding propertyBinding() {
//        return BindingBuilder
//                .bind(propertyQueue())
//                .to(propertyExchange())
//                .with("iot.routing.property.*");
//    }


//    @Bean
//    TopicExchange eventExchange() {
//        return new TopicExchange("iot.exchange.event", true, false);
//    }
//
//    @Bean
//    Queue eventQueue() {
//        Map<String, Object> arguments = new HashMap<>();
//        // 15秒：15 * 1000 = 15000L
//        arguments.put("x-message-ttl", 15000L);
//        return new Queue("iot.queue.event", true, false, false, arguments);
//    }
//
//    @Bean
//    Binding eventBinding() {
//        return BindingBuilder
//                .bind(eventQueue())
//                .to(eventExchange())
//                .with("iot.routing.event.*");
//    }
}
