package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 店铺列表聚合统计响应（店铺基础字段 + 每店会员聚合）
 */
@Data
@Schema(description = "店铺列表聚合统计响应")
public class ShopStatsVO {

    @Schema(description = "店铺ID")
    private Long id;

    @Schema(description = "店铺名称")
    private String name;

    @Schema(description = "店铺Logo URL")
    private String logo;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "营业时间")
    private String businessHours;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "未删除会员数量")
    private Integer memberCount;

    @Schema(description = "未删除会员储值余额合计（元，保留2位）")
    private BigDecimal totalBalance;

}