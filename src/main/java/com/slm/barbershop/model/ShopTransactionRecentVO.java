package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 店铺最近交易记录(右侧"最新动态"使用)
 */
@Data
@Schema(description = "店铺最近交易记录")
public class ShopTransactionRecentVO {

    @Schema(description = "交易ID")
    private Long id;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "会员姓名")
    private String memberName;

    @Schema(description = "交易类型:STORE-储值 CONSUME-消费")
    private String type;

    @Schema(description = "交易金额")
    private BigDecimal amount;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

}
