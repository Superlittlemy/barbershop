package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 账单汇总(店铺维度)
 */
@Data
@Schema(description = "账单汇总")
public class BillSummaryVO {

    @Schema(description = "今日金额")
    private BigDecimal todayAmount;

    @Schema(description = "今日笔数")
    private Long todayCount;

    @Schema(description = "本月金额")
    private BigDecimal monthAmount;

    @Schema(description = "本月笔数")
    private Long monthCount;

    @Schema(description = "各支付方式金额(key=payChannel)")
    private Map<String, BigDecimal> byChannelAmount;

    @Schema(description = "各支付方式笔数(key=payChannel)")
    private Map<String, Long> byChannelCount;

}
