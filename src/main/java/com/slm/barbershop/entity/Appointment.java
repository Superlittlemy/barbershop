package com.slm.barbershop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 会员预约
 * <p>
 * 本期极简:无状态机,创建即生效。{@code status} 列仅作兜底(0=有效),不暴露给店家操作。
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
     * 状态:0=有效(本期唯一值);预留扩展
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

}
