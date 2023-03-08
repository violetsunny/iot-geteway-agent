package top.iot.gateway.agent;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.annotation.EnableEasyormRepository;
import org.hswebframework.web.logging.aop.EnableAccessLogger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;
import reactor.core.publisher.Hooks;


@SpringBootApplication(scanBasePackages = "top.iot", exclude = {
    DataSourceAutoConfiguration.class
})
@EnableDiscoveryClient
@EnableCaching
@EnableKafka
@EnableAccessLogger
@EnableEasyormRepository("top.iot.**.entity")
@EnableFeignClients(basePackages = {"top.iot.gateway"})
@Slf4j
public class GatewayAgentApplication {

    public static void main(String[] args) {
        Hooks.onOperatorDebug();
        SpringApplication.run(GatewayAgentApplication.class, args);
    }

}
