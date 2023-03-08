package top.iot.gateway.agent.web;

import top.iot.gateway.agent.configuration.EmqxConfiguration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;

@RequestMapping("/emqx")
@RestController
@Tag(name = "接入信息")
public class EmqxInfoController {

    @Resource
    EmqxConfiguration emqxConfiguration;

    @GetMapping("/info")
    @Operation(summary = "获取emqx连接信息")
    public Mono<EmqxInfoVO> getInfo() {
        EmqxInfoVO info = new EmqxInfoVO(emqxConfiguration.getHost(),emqxConfiguration.getPort(),false);
        return Mono.just(info);
    }
    @Data
    @AllArgsConstructor
    class EmqxInfoVO{
        String host;
        String port;
        boolean TLS;
    }
}







