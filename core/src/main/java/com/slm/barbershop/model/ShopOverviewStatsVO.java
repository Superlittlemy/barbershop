package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "概览页 KPI 聚合数据")
public class ShopOverviewStatsVO {

    @Schema(description = "店铺数量")
    private Integer shopCount;

    @Schema(description = "会员数量")
    private Integer memberCount;

    @Schema(description = "会员余额合计")
    private BigDecimal totalBalance;

    @Schema(description = "近30天消费金额")
    private BigDecimal recentTxAmount;

    @Schema(description = "统计截止时间")
    private LocalDateTime asOf;

}
