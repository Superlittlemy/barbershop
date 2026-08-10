package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 账单响应
 */
@Data
@Schema(description = "账单响应")
public class BillResponse {

    @Schema(description = "账单ID")
    private Long id;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "会员姓名(实时查询)")
    private String memberName;

    @Schema(description = "会员手机号(实时查询)")
    private String memberPhone;

    @Schema(description = "支付方式")
    private String payChannel;

    @Schema(description = "账单类型:CONSUME-消费 STORE-储值")
    private String type;

    @Schema(description = "账单总金额")
    private BigDecimal totalAmount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否作废:0.正常 1.已作废")
    private Integer isCancelled;

    @Schema(description = "作废时间")
    private LocalDateTime cancelledTime;

    @Schema(description = "作废操作人")
    private Long cancelledBy;

    @Schema(description = "作废原因")
    private String cancelReason;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "服务项目明细")
    private List<TransactionItemResponse> items;

}
