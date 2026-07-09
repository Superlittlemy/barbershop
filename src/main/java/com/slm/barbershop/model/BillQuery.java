package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 账单查询条件(Controller 层入参)
 */
@Data
@Schema(description = "账单查询条件")
public class BillQuery {

    @Schema(description = "支付方式过滤")
    private String payChannel;

    @Schema(description = "会员ID过滤")
    private Long memberId;

    @Schema(description = "客户姓名(LIKE)")
    private String customerName;

    @Schema(description = "客户手机号(LIKE)")
    private String customerPhone;

    @Schema(description = "起始时间(包含)")
    private LocalDateTime startTime;

    @Schema(description = "结束时间(包含)")
    private LocalDateTime endTime;

    @Schema(description = "是否包含已作废的账单(默认 false)")
    private Boolean includeCancelled;

    public boolean includeCancelled() {
        return includeCancelled != null && includeCancelled;
    }

}
