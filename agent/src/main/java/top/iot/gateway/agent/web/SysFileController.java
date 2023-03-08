package top.iot.gateway.agent.web;

import top.iot.gateway.component.gateway.external.SysFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.multipart.Part;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


/**
 * @author ruanhong
 */
@RestController
@RequestMapping("/file")
@Tag(name = "文件上传")
public class SysFileController {

    @Autowired
    private SysFileService sysFileService;

    /**
     * 文件上传minio
     *
     * @param part 文件
     * @return 返回
     */
    @PostMapping("/static")
    @SneakyThrows
    @Operation(summary = "上传文件")
    public Mono<String> uploadStatic(@RequestPart("file")
                                     @Parameter(name = "file", description = "文件", style = ParameterStyle.FORM) Part part) {
        return sysFileService.uploadFileMinio(part);

    }


}
