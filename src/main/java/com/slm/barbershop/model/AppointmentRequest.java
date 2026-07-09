package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 预约请求(会员创建预约)
 */
@Data
@Schema(description = "预约请求")
public class AppointmentRequest {

    @NotNull(message = "店铺ID不能为空")
    @Schema(description = "店铺ID")
    private Long shopId;

    @NotNull(message = "服务项目ID不能为空")
    @Schema(description = "服务项目ID")
    private Long serviceItemId;

    @NotNull(message = "预约日期不能为空")
    @Schema(description = "预约日期(yyyy-MM-dd)")
    private LocalDate appointmentDate;

    @NotNull(message = "起始时段不能为空")
    @Schema(description = "起始时段(HH:mm)")
    private String startTime;

    @Schema(description = "备注")
    private String remark;

}
