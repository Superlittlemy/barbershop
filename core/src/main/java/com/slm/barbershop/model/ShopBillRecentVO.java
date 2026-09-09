package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 店铺最近账单(供店铺详情"最新动态"侧栏使用)
 */
@Data
@Schema(description = "店铺最近账单")
public class ShopBillRecentVO {

    @Schema(description = "账单ID")
    private Long id;

    @Schema(description = "支付方式")
    private String payChannel;

    @Schema(description = "账单金额")
    private BigDecimal totalAmount;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "会员姓名(开单时快照)")
    private String memberName;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

}
