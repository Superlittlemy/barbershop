package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 交易明细(消费项目)响应
 */
@Data
@Schema(description = "交易明细(消费项目)响应")
public class TransactionItemResponse {

    @Schema(description = "明细ID")
    private Long id;

    @Schema(description = "关联交易ID")
    private Long transactionId;

    @Schema(description = "项目ID")
    private Long itemId;

    @Schema(description = "项目名(快照)")
    private String itemName;

    @Schema(description = "下单时单价(快照)")
    private BigDecimal unitPrice;

    @Schema(description = "数量")
    private Integer quantity;

    @Schema(description = "小计")
    private BigDecimal subtotal;

    @Schema(description = "排序号")
    private Integer sortNo;

}
