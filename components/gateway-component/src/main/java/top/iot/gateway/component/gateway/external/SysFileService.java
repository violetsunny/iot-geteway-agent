package top.iot.gateway.component.gateway.external;


import top.iot.gateway.component.common.utils.MinIoClientConfig;
import top.iot.gateway.component.common.utils.MinioUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Locale;

/**
 * @author ruanhong
 */
@Service
@Slf4j
public class SysFileService {
    @Autowired
    private MinIoClientConfig minioConfig;

    @Autowired
    private MinioUtil minioUtil;


    public Mono<String> uploadFileMinio(Part part) {
        log.info("uploadFileMinio file start");
        if (part instanceof FilePart) {
            FilePart filePart = ((FilePart) part);
            // 生成文件名
            String fineName = filePart.filename();
            String suffix = (fineName.contains(".") ? fineName.substring(fineName.lastIndexOf(".") + 1) : "").toLowerCase(Locale.ROOT);

            if (!"jar".equalsIgnoreCase(suffix)) {
                return Mono.error(() -> new RuntimeException("Illegal file format : must be jar files"));
            }
            // 判断存储桶是否存在
            if (!minioUtil.existBucket(minioConfig.getBucket())) {
                minioUtil.makeBucket(minioConfig.getBucket());
            }
            try {
                // 上传文件
                return minioUtil.uploadOne(filePart);
                //return Mono.just(minioConfig.getEndpoint() + "/" + minioConfig.getBucketName() + "/" + fileNameNew);
            } catch (Exception e) {
                log.error("uploadFileMinio fail", e);
                return Mono.error(() -> new RuntimeException("[file] part upload fail"));
            }
        } else {
            return Mono.error(() -> new IllegalArgumentException("[file] part is not a file"));
        }

    }


}
