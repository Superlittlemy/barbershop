package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 会员交易请求
 */
@Data
@Schema(description = "会员交易请求")
public class MemberTransactionRequest {

    @Schema(description = "交易金额(当 items 非空时,本字段会被按项目汇总值覆盖)")
    private BigDecimal amount;

    @Schema(description = "员工ID(消费时必填;储值忽略)")
    private Long employeeId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "幂等键")
    private String idempotencyKey;

    @Schema(description = "消费项目明细(仅消费有效;非空时按 unitPrice * quantity 汇总金额)")
    private List<TransactionItemRequest> items;

}
