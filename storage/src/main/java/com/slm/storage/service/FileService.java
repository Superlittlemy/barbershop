package com.slm.storage.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.common.enums.ResultStatus;
import com.slm.common.exception.BizException;
import com.slm.storage.config.MinioConfig;
import com.slm.storage.entity.FileMetadata;
import com.slm.storage.mapper.FileMetadataMapper;
import com.slm.storage.model.AccessUrlResponse;
import com.slm.storage.model.FileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileService extends ServiceImpl<FileMetadataMapper, FileMetadata> {

    private static final long MAX_SIZE = 20L * 1024 * 1024;

    /**
     * 单次批量换取访问地址的objectKey数上限
     */
    private static final int MAX_ACCESS_URL_BATCH = 100;

    /**
     * 预签名有效期下限(秒)
     */
    private static final int MIN_EXPIRY_SECONDS = 60;

    /**
     * 预签名有效期上限(秒):MinIO预签名URL硬限制7天
     */
    private static final int MAX_EXPIRY_SECONDS = 7 * 24 * 3600;

    /**
     * 预签名有效期缺省值(秒):配置未提供时的兜底
     */
    private static final int DEFAULT_EXPIRY_SECONDS = 1800;

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Autowired
    private MinioService minioService;

    @Autowired
    private MinioConfig.MinioProperties minioProperties;

    public FileResponse upload(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultStatus.BAD_REQUEST, "文件为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException(ResultStatus.FILE_SIZE_EXCEEDED, "文件大小不能超过 20MB");
        }
        // userId 现仅用于审计/后续扩展,createdBy 由 EntityMetadataConfig 从 UserContext 自动填

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("读取文件失败", e);
            throw new BizException(ResultStatus.FILE_UPLOAD_ERROR, "读取文件失败: " + e.getMessage());
        }
        String md5 = DigestUtils.md5DigestAsHex(bytes);

        FileMetadata existing = this.lambdaQuery()
                .eq(FileMetadata::getMd5, md5)
                .one();
        if (existing != null) {
            return toResponse(existing);
        }

        this.getBaseMapper().physicalDeleteSoftDeletedByMd5(md5);

        String ext = extractExtension(file.getOriginalFilename());
        String objectKey = LocalDate.now().format(DATE_PATH) + "/" + UUID.randomUUID() + ext;

        FileMetadata entity = new FileMetadata();
        entity.setOriginalName(file.getOriginalFilename());
        entity.setContentType(file.getContentType());
        entity.setSizeBytes(file.getSize());
        entity.setMd5(md5);
        entity.setObjectKey(objectKey);

        try {
            minioService.putObject(file, objectKey);
            this.save(entity);
            return toResponse(entity);
        } catch (DuplicateKeyException dup) {
            minioService.removeObject(objectKey);
            FileMetadata winner = this.lambdaQuery()
                    .eq(FileMetadata::getMd5, md5)
                    .one();
            if (winner == null) {
                throw dup;
            }
            return toResponse(winner);
        }
    }

    /**
     * 批量获取带过期时间的文件访问地址
     * <p>
     * 仅返回 file_metadata 中存在的 key,不存在的 key 跳过不报错(调用方回退为不展示)。
     */
    public List<AccessUrlResponse> getAccessUrls(List<String> objectKeys, Integer expiresSeconds) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            throw new BizException(ResultStatus.BAD_REQUEST, "objectKey不能为空");
        }
        if (objectKeys.size() > MAX_ACCESS_URL_BATCH) {
            throw new BizException(ResultStatus.BAD_REQUEST, "objectKey数量不能超过" + MAX_ACCESS_URL_BATCH);
        }

        int expiry = resolveExpiry(expiresSeconds);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expiry);

        List<String> distinctKeys = objectKeys.stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        Map<String, FileMetadata> existing = new HashMap<>();
        if (!distinctKeys.isEmpty()) {
            this.lambdaQuery()
                    .in(FileMetadata::getObjectKey, distinctKeys)
                    .list()
                    .forEach(e -> existing.put(e.getObjectKey(), e));
        }

        List<AccessUrlResponse> result = new ArrayList<>();
        for (String key : distinctKeys) {
            if (!existing.containsKey(key)) {
                continue;
            }
            AccessUrlResponse r = new AccessUrlResponse();
            r.setObjectKey(key);
            r.setAccessUrl(minioService.getPresignedUrl(key, expiry));
            r.setExpiresAt(expiresAt);
            result.add(r);
        }
        return result;
    }

    private int resolveExpiry(Integer expiresSeconds) {
        Integer configured = minioProperties.getPresignExpirationSeconds();
        int expiry = expiresSeconds != null ? expiresSeconds
                : (configured != null ? configured : DEFAULT_EXPIRY_SECONDS);
        return Math.max(MIN_EXPIRY_SECONDS, Math.min(expiry, MAX_EXPIRY_SECONDS));
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 || dot == filename.length() - 1 ? "" : filename.substring(dot);
    }

    private FileResponse toResponse(FileMetadata entity) {
        FileResponse r = new FileResponse();
        r.setId(entity.getId());
        r.setOriginalName(entity.getOriginalName());
        r.setContentType(entity.getContentType());
        r.setSizeBytes(entity.getSizeBytes());
        r.setMd5(entity.getMd5());
        r.setObjectKey(entity.getObjectKey());
        r.setCreatedTime(entity.getCreatedTime());
        return r;
    }

}
