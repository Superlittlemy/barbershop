package com.slm.barbershop.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.slm.barbershop.entity.Bill;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 账单查询条件(Controller 层入参)
 * <p>
 * 继承 {@link com.baomidou.mybatisplus.extension.plugins.pagination.Page}，实现“分页 + 查询”合一。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "账单查询条件(含分页)")
public class BillQuery extends Page<Bill> {

    @Schema(description = "店铺ID(用于数据隔离与鉴权)")
    private Long shopId;

    @Schema(description = "支付方式过滤")
    private String payChannel;

    @Schema(description = "账单类型过滤:CONSUME-消费 / STORE-储值")
    private String type;

    @Schema(description = "会员ID过滤")
    private Long memberId;

    @Schema(description = "员工ID过滤")
    private Long employeeId;

    @Schema(description = "起始时间(包含)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startTime;

    @Schema(description = "结束时间(包含)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endTime;

    @Schema(description = "是否包含已作废的账单(默认 false)")
    private Boolean includeCancelled;

    public boolean includeCancelled() {
        return includeCancelled != null && includeCancelled;
    }

}
