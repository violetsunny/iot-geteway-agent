package top.iot.gateway.component.common.utils;

import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.utils.time.DateFormatter;
import org.hswebframework.web.id.IDGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ruanhong
 */

@Component
@Slf4j
public class MinioUtil {
    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinIoClientConfig minioConfig;

    /**
     * description: 判断bucket是否存在，不存在则创建
     *
     * @return: void
     */
    public boolean existBucket(String name) {

        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(name).build());
        } catch (Exception e) {
            log.error("existBucket error", e);
        }
        return false;

    }

    /**
     * 创建存储bucket
     *
     * @param bucketName 存储bucket名称
     * @return Boolean
     */
    public Boolean makeBucket(String bucketName) {
        try {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            log.error("makeBucket error", e);
            return false;
        }
        return true;
    }

    /**
     * 删除存储bucket
     *
     * @param bucketName 存储bucket名称
     * @return Boolean
     */
    public Boolean removeBucket(String bucketName) {
        try {
            minioClient.removeBucket(RemoveBucketArgs.builder()
                    .bucket(bucketName)
                    .build());
        } catch (Exception e) {
            log.error("removeBucket error", e);
            return false;
        }
        return true;
    }

    /**
     * description: 上传文件
     *
     * @param multipartFile
     * @return: java.lang.String
     */
    public List<String> upload(MultipartFile[] multipartFile) {
        List<String> names = new ArrayList<>(multipartFile.length);
        for (MultipartFile file : multipartFile) {
            String fileName = file.getOriginalFilename();
            String[] split = fileName.split("\\.");
            if (split.length > 1) {
                fileName = split[0] + "_" + System.currentTimeMillis() + "." + split[1];
            } else {
                fileName = fileName + System.currentTimeMillis();
            }
            InputStream in = null;
            try {
                in = file.getInputStream();
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(fileName)
                        .stream(in, in.available(), -1)
                        .contentType(file.getContentType())
                        .build()
                );
            } catch (Exception e) {
                log.error("upload error", e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        log.error("upload error", e);
                    }
                }
            }
            names.add(fileName);
        }
        return names;
    }

    public Mono<String> uploadOne(FilePart filePart) {

        return filePart.content().map(DataBuffer::asInputStream).reduce(SequenceInputStream::new).flatMap(inputStream -> {
            try {
                String fileName = generateFileName(filePart.filename());
                PutObjectArgs putObjectRequest = PutObjectArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .object(fileName)
                        .stream(inputStream, inputStream.available(), -1)
                        .build();
                // 高级接口会返回一个异步结果Upload
                // 可同步地调用 waitForUploadResult 方法等待上传完成，成功返回UploadResult, 失败抛出异常
                minioClient.putObject(putObjectRequest);
                return Mono.just(minioConfig.getResourcesOutUrl() + "/" + minioConfig.getBucket() + "/" + fileName);
            } catch (Exception e) {
                log.error("文件上传出错", e);
                return Mono.error(e);
            }
        });
    }

    public String generateFileName(String oriName) {
        String fileName = IDGenerator.SNOW_FLAKE_STRING.generate();
        String filePath = DateFormatter.toString(new Date(), "yyyyMMdd");
        //文件后缀
        String suffix = oriName.contains(".") ?
                oriName.substring(oriName.lastIndexOf(".")) : "";
        return filePath + fileName + suffix;


    }
//    /**
//     * description: 下载文件
//     *
//     * @param fileName
//     * @return: org.springframework.http.ResponseEntity<byte [ ]>
//     */
//    public ResponseEntity<byte[]> download(String fileName) {
//        ResponseEntity<byte[]> responseEntity = null;
//        InputStream in = null;
//        ByteArrayOutputStream out = null;
//        try {
//            in = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(fileName).build());
//            out = new ByteArrayOutputStream();
//            IOUtils.copy(in, out);
//            //封装返回值
//            byte[] bytes = out.toByteArray();
//            HttpHeaders headers = new HttpHeaders();
//            try {
//                headers.add("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
//            } catch (UnsupportedEncodingException e) {
//                log.error(e);
//            }
//            headers.setContentLength(bytes.length);
//            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
//            headers.setAccessControlExposeHeaders(Arrays.asList("*"));
//            responseEntity = new ResponseEntity<byte[]>(bytes, headers, HttpStatus.OK);
//        } catch (Exception e) {
//            log.error(e);
//        } finally {
//            try {
//                if (in != null) {
//                    try {
//                        in.close();
//                    } catch (IOException e) {
//                        log.error(e);
//                    }
//                }
//                if (out != null) {
//                    out.close();
//                }
//            } catch (IOException e) {
//                log.error(e);
//            }
//        }
//        return responseEntity;
//    }

    /**
     * 查看文件对象
     *
     * @param bucketName 存储bucket名称
     * @return 存储bucket内文件对象信息
     */
    public List<ObjectItem> listObjects(String bucketName) {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder().bucket(bucketName).build());
        List<ObjectItem> objectItems = new ArrayList<>();
        try {
            for (Result<Item> result : results) {
                Item item = result.get();
                ObjectItem objectItem = new ObjectItem();
                objectItem.setObjectName(item.objectName());
                objectItem.setSize(item.size());
                objectItems.add(objectItem);
            }
        } catch (Exception e) {
            log.error("listObjects error", e);
            return null;
        }
        return objectItems;
    }

    /**
     * 批量删除文件对象
     *
     * @param bucketName 存储bucket名称
     * @param objects    对象名称集合
     */
    public Iterable<Result<DeleteError>> removeObjects(String bucketName, List<String> objects) {
        List<DeleteObject> dos = objects.stream().map(e -> new DeleteObject(e)).collect(Collectors.toList());
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(RemoveObjectsArgs.builder().bucket(bucketName).objects(dos).build());
        return results;
    }


}


