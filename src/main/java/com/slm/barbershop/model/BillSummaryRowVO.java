package com.slm.barbershop.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * BillMapper.summarizeByShopId 内部行结果:
 * 一个 payChannel 可能产出 TODAY / MONTH 两行(分别聚合)
 */
@Data
public class BillSummaryRowVO {

    /** 桶:TODAY / MONTH;null 表示全期(整体 byChannel) */
    private String bucket;

    /** 支付方式 */
    private String payChannel;

    /** 该桶该渠道的金额 */
    private BigDecimal amount;

    /** 该桶该渠道的笔数 */
    private Long count;

}
