package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 会员交易请求
 */
@Data
@Schema(description = "会员交易请求")
public class MemberTransactionRequest {

    @Schema(description = "交易金额")
    private BigDecimal amount;

    @Schema(description = "备注")
    private String remark;

}