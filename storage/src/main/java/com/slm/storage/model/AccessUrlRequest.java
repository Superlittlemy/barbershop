package com.slm.storage.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 批量获取文件访问地址请求
 * <p>
 * 客户端持 objectKey 换取带过期时间的临时访问地址,不直接持有存储基础设施地址。
 */
@Data
@Schema(description = "批量获取文件访问地址请求")
public class AccessUrlRequest {

    @NotEmpty
    @Size(max = 100)
    @Schema(description = "MinIO对象Key列表(单次最多100个)", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> objectKeys;

    @Schema(description = "访问地址有效期(秒);可选,缺省取服务端默认值(30分钟),上限7天")
    private Integer expiresSeconds;

}
