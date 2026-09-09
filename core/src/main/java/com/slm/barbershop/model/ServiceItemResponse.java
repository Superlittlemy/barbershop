package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 消费项目响应
 */
@Data
@Schema(description = "消费项目响应")
public class ServiceItemResponse {

    @Schema(description = "项目ID")
    private Long id;

    @Schema(description = "所属店铺ID")
    private Long shopId;

    @Schema(description = "所属分类ID")
    private Long categoryId;

    @Schema(description = "所属分类名称")
    private String categoryName;

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "默认单价")
    private BigDecimal price;

    @Schema(description = "项目描述")
    private String description;

    @Schema(description = "状态:0.下架 1.上架")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

}
