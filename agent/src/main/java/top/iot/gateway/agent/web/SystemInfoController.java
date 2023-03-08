package top.iot.gateway.agent.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import top.iot.gateway.component.common.Version;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequestMapping("/system")
@RestController
@Tag(name = "系统管理")
public class SystemInfoController {

    @GetMapping("/version")
    public Mono<Version> getVersion() {
        return Mono.just(Version.current);
    }

}
