package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 消费项目请求
 */
@Data
@Schema(description = "消费项目请求")
public class ServiceItemRequest {

    @Schema(description = "所属店铺ID")
    private Long shopId;

    @Schema(description = "所属分类ID(可空)")
    private Long categoryId;

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "默认单价")
    private BigDecimal price;

    @Schema(description = "项目描述")
    private String description;

    @Schema(description = "状态:0.下架 1.上架")
    private Integer status;

}
