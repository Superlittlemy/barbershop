package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消费项目分类响应
 */
@Data
@Schema(description = "消费项目分类响应")
public class ServiceCategoryResponse {

    @Schema(description = "分类ID")
    private Long id;

    @Schema(description = "所属店铺ID")
    private Long shopId;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "排序号")
    private Integer sortNo;

    @Schema(description = "状态:0.停用 1.启用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

}
