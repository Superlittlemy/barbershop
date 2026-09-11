package com.slm.barbershop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.slm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 会员预约
 * <p>
 * 状态由 {@link com.slm.barbershop.enums.AppointmentStatus} 描述:0=有效(创建即生效)、1=已取消(会员自助取消)。
 * 取消后的预约({@code status=1})不占用时段,可被重新预约;店家分页查询可见全部状态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("appointment")
public class Appointment extends BaseEntity {

    /**
     * 所属店铺ID
     */
    private Long shopId;

    /**
     * 会员ID
     */
    private Long memberId;

    /**
     * 员工ID(可空:未指定员工)
     */
    private Long employeeId;

    /**
     * 服务项目ID
     */
    private Long serviceItemId;

    /**
     * 预约日期(yyyy-MM-dd)
     */
    private LocalDate appointmentDate;

    /**
     * 起始时段(HH:mm:ss)
     */
    private LocalTime startTime;

    /**
     * 结束时段(HH:mm:ss,固定为 startTime + 1 小时)
     */
    private LocalTime endTime;

    /**
     * 状态:0=有效(AppointmentStatus.VALID,创建初始态)、1=已取消(AppointmentStatus.CANCELLED,会员自助取消后的终态)。
     * 取消后的预约不再占用时段,可被重新预约。
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

}
