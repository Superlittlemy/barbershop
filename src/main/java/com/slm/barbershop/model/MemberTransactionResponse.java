package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员交易响应
 */
@Data
@Schema(description = "会员交易响应")
public class MemberTransactionResponse {

    @Schema(description = "交易ID")
    private Long id;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "交易类型")
    private String type;

    @Schema(description = "交易金额")
    private BigDecimal amount;

    @Schema(description = "交易前余额")
    private BigDecimal balanceBefore;

    @Schema(description = "交易后余额")
    private BigDecimal balanceAfter;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "消费项目明细(消费时存在,储值为空列表)")
    private List<TransactionItemResponse> items;

}
