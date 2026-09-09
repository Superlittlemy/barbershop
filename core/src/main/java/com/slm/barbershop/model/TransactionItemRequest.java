package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 交易明细(消费项目)请求
 */
@Data
@Schema(description = "交易明细(消费项目)请求")
public class TransactionItemRequest {

    @Schema(description = "消费项目ID")
    private Long itemId;

    @Schema(description = "数量(1~999)")
    private Integer quantity;

    @Schema(description = "下单时单价(可选;为空时使用 service_item.price)")
    private BigDecimal unitPrice;

}