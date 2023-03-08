package top.iot.gateway.agent.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "emqx")
@Data
public class EmqxConfiguration {
    String host;
    String port;
    boolean TLS;
    String userName;
    String password;
}

