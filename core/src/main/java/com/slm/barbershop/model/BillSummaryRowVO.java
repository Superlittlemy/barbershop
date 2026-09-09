package com.slm.barbershop.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * BillMapper.summarizeByShopId 内部行结果:
 * 一个 pay_channel 一行,同时给出 today / month 两个口径的金额与笔数。
 * <ul>
 * <li>todayAmount / todayCount: 今天 00:00:00 之后</li>
 * <li>monthAmount / monthCount: 本月 1 号 00:00:00 之后(含今天),即"本月累计"</li>
 * </ul>
 */
@Data
public class BillSummaryRowVO {

    /** 支付方式 */
    private String payChannel;

    /** 该渠道今天的金额 */
    private BigDecimal todayAmount;

    /** 该渠道今天的笔数 */
    private Long todayCount;

    /** 该渠道本月累计金额(月初 00:00:00 至今) */
    private BigDecimal monthAmount;

    /** 该渠道本月累计笔数(月初 00:00:00 至今) */
    private Long monthCount;

}