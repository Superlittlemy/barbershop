package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 消费项目分类请求
 */
@Data
@Schema(description = "消费项目分类请求")
public class ServiceCategoryRequest {

    @NotNull(message = "所属店铺ID不能为空")
    @Schema(description = "所属店铺ID")
    private Long shopId;

    @NotBlank(message = "分类名称不能为空")
    @Schema(description = "分类名称")
    private String name;

    @NotNull(message = "排序号不能为空")
    @Schema(description = "排序号")
    private Integer sortNo;

    @NotNull(message = "状态不能为空")
    @Schema(description = "状态:0.停用 1.启用")
    private Integer status;

}
