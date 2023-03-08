package top.iot.gateway.manager.web;

import top.iot.gateway.manager.service.LocalDeviceInstanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
@RequestMapping({"/device-instance", "/device/instance"})
@Resource(id = "device-instance", name = "设备实例")
@Slf4j
@Tag(name = "设备实例接口")
public class DeviceInstanceController {

    @Getter
    @Autowired
    private LocalDeviceInstanceService service;

    //设备功能调用
    @PostMapping("/{deviceId:.+}/function/{functionId}")
    @SneakyThrows
    @QueryAction
    @Operation(summary = "发送调用设备功能指令到设备", description = "请求示例: {\"参数\":\"值\"}")
    public Flux<?> invokedFunction(@PathVariable String deviceId,
                                   @PathVariable String functionId,
                                   @RequestBody Mono<Map<String, Object>> properties) {

        return properties.flatMapMany(props -> service.invokeFunction(deviceId, functionId, props));
    }
}
