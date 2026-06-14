package com.slm.barbershop.service;

import com.slm.barbershop.config.MinioConfig;
import com.slm.barbershop.exception.BizException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.InputStream;

@Slf4j
@Service
public class MinioService {

    private static final String PUBLIC_READ_POLICY_TEMPLATE =
            "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"AWS\":[\"*\"]},\"Action\":[\"s3:GetObject\"],\"Resource\":[\"arn:aws:s3:::%s/*\"]}]}";

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
            String policy = String.format(PUBLIC_READ_POLICY_TEMPLATE, bucket);
            minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
            log.info("MinIO bucket policy set to public-read: {}", bucket);
        } catch (Exception e) {
            log.error("MinIO 初始化失败", e);
            throw new BizException(HttpStatus.INTERNAL_SERVER_ERROR, "MinIO 初始化失败: " + e.getMessage());
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
            throw new BizException(HttpStatus.INTERNAL_SERVER_ERROR, "文件存储失败: " + e.getMessage());
        }
    }

    /**
     * 构造公开访问URL
     */
    public String buildPublicUrl(String objectKey) {
        return props.getPublicBaseUrl() + "/" + props.getBucket() + "/" + objectKey;
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
