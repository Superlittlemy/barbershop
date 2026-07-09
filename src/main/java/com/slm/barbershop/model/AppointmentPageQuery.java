package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 预约分页查询条件
 * <p>
 * date 非空时按日期过滤;keyword 非空时按会员姓名/服务项目名模糊匹配。
 */
@Data
@Schema(description = "预约分页查询")
public class AppointmentPageQuery {

    @NotNull(message = "店铺ID不能为空")
    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "预约日期(yyyy-MM-dd,可选)")
    private LocalDate date;

    @Schema(description = "关键词:会员姓名/服务项目名模糊匹配(可选)")
    private String keyword;

}
