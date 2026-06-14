package com.slm.barbershop.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.entity.FileMetadata;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.FileMetadataMapper;
import com.slm.barbershop.model.FileResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
public class FileService extends ServiceImpl<FileMetadataMapper, FileMetadata> {

    private static final long MAX_SIZE = 20L * 1024 * 1024;

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    @Autowired
    private MinioService minioService;

    public FileResponse upload(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BizException(HttpStatus.BAD_REQUEST, "文件为空");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException(HttpStatus.PAYLOAD_TOO_LARGE, "文件大小不能超过 20MB");
        }
        // userId 现仅用于审计/后续扩展,createdBy 由 EntityMetadataConfig 从 UserContext 自动填

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            log.error("读取文件失败", e);
            throw new BizException(HttpStatus.INTERNAL_SERVER_ERROR, "读取文件失败: " + e.getMessage());
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
        String accessUrl = minioService.buildPublicUrl(objectKey);

        FileMetadata entity = new FileMetadata();
        entity.setOriginalName(file.getOriginalFilename());
        entity.setContentType(file.getContentType());
        entity.setSizeBytes(file.getSize());
        entity.setMd5(md5);
        entity.setObjectKey(objectKey);
        entity.setAccessUrl(accessUrl);

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

    public FileResponse getById(Long id) {
        FileMetadata entity = this.lambdaQuery().eq(FileMetadata::getId, id).one();
        if (entity == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "文件不存在");
        }
        return toResponse(entity);
    }

    public void delete(Long id, Long userId) {
        FileMetadata entity = this.lambdaQuery().eq(FileMetadata::getId, id).one();
        if (entity == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "文件不存在");
        }
        if (userId != null && entity.getCreatedBy() != null && !entity.getCreatedBy().equals(userId)) {
            throw new BizException(HttpStatus.FORBIDDEN, "无权删除该文件");
        }
        this.removeById(id);
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
        r.setAccessUrl(entity.getAccessUrl());
        r.setCreatedTime(entity.getCreatedTime());
        return r;
    }

}
