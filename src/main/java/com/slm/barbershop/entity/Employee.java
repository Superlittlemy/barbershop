package com.slm.barbershop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 店铺员工实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("employee")
public class Employee extends BaseEntity {

    /**
     * 所属店铺ID
     */
    private Long shopId;

    /**
     * 工号(系统生成,店内递增,如E0001)
     */
    private String employeeNo;

    /**
     * 姓名
     */
    private String name;

    /**
     * 性别:1.男 2.女(可空)
     */
    private Integer gender;

    /**
     * 入职时间
     */
    private LocalDate hireDate;

}
