package com.slm.storage.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "文件响应")
public class FileResponse {

    @Schema(description = "文件ID")
    private Long id;

    @Schema(description = "原始文件名")
    private String originalName;

    @Schema(description = "MIME类型")
    private String contentType;

    @Schema(description = "文件大小(字节)")
    private Long sizeBytes;

    @Schema(description = "MD5(小写hex)")
    private String md5;

    @Schema(description = "MinIO对象Key")
    private String objectKey;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

}
