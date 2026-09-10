package com.slm.storage.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "文件访问地址响应")
public class AccessUrlResponse {

    @Schema(description = "MinIO对象Key")
    private String objectKey;

    @Schema(description = "带过期时间的访问地址(GET预签名URL)")
    private String accessUrl;

    @Schema(description = "访问地址过期时间")
    private LocalDateTime expiresAt;

}
