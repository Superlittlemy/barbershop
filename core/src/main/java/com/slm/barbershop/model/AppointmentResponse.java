package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 预约响应
 */
@Data
@Schema(description = "预约响应")
public class AppointmentResponse {

    @Schema(description = "预约ID")
    private Long id;

    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "会员姓名")
    private String memberName;

    @Schema(description = "会员手机号(实时查询)")
    private String memberPhone;

    @Schema(description = "员工ID(可空:未指定员工)")
    private Long employeeId;

    @Schema(description = "员工姓名(实时查询)")
    private String employeeName;

    @Schema(description = "服务项目ID")
    private Long serviceItemId;

    @Schema(description = "服务项目名称")
    private String serviceItemName;

    @Schema(description = "预约日期(yyyy-MM-dd)")
    private LocalDate appointmentDate;

    @Schema(description = "起始时段(HH:mm:ss)")
    private LocalTime startTime;

    @Schema(description = "结束时段(HH:mm:ss)")
    private LocalTime endTime;

    @Schema(description = "状态:0=有效(AppointmentStatus.VALID)、1=已取消(AppointmentStatus.CANCELLED)")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

}
