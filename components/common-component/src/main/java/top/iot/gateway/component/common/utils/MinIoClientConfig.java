package top.iot.gateway.component.common.utils;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author ruanhong
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinIoClientConfig {

    private String accessKeyId;

    private String accessKeySecret;

    private String bucket;
    /**
     * 上传文件的baseURL
     */
    private String resourcesUrl;
    /**
     * 外部访问查看的baseURL
     */
    private String resourcesOutUrl;

    /**
     * 注入minio 客户端
     *
     * @return
     */
    @Bean
    public MinioClient minioClient() {

        return MinioClient.builder()
                .endpoint(resourcesUrl)
                .credentials(accessKeyId, accessKeySecret)
                .build();
    }
}

