package com.slm.storage.service;

import com.slm.common.enums.ResultStatus;
import com.slm.common.exception.BizException;
import com.slm.storage.config.MinioConfig;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MinioService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioConfig.MinioProperties props;

    @PostConstruct
    public void init() {
        String bucket = props.getBucket();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket created: {}", bucket);
            }
        } catch (Exception e) {
            log.error("MinIO 初始化失败", e);
            throw new BizException(ResultStatus.MINIO_INIT_ERROR, "MinIO 初始化失败: " + e.getMessage());
        }
    }

    /**
     * 上传对象
     */
    public void putObject(MultipartFile file, String objectKey) {
        try (InputStream in = file.getInputStream()) {
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .stream(in, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();
            minioClient.putObject(args);
        } catch (Exception e) {
            log.error("MinIO 上传失败: objectKey={}", objectKey, e);
            throw new BizException(ResultStatus.MINIO_OPERATION_ERROR, "文件存储失败: " + e.getMessage());
        }
    }

    /**
     * 生成带过期时间的GET预签名URL(私有bucket文件访问的唯一入口)
     */
    public String getPresignedUrl(String objectKey, int expirySeconds) {
        try {
            GetPresignedObjectUrlArgs args = GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .expiry(expirySeconds, TimeUnit.SECONDS)
                    .build();
            return minioClient.getPresignedObjectUrl(args);
        } catch (Exception e) {
            log.error("MinIO 生成预签名URL失败: objectKey={}", objectKey, e);
            throw new BizException(ResultStatus.MINIO_OPERATION_ERROR, "生成文件访问地址失败: " + e.getMessage());
        }
    }

    /**
     * 删除对象(本版本未使用,保留以备未来清理)
     */
    public void removeObject(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.warn("MinIO 删除对象失败: objectKey={}", objectKey, e);
        }
    }

}
