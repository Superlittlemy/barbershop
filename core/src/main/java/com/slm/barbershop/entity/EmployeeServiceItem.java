package com.slm.barbershop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 员工-服务项目关联(多选)
 */
@Data
@TableName("employee_service_item")
public class EmployeeServiceItem {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("employee_id")
    private Long employeeId;

    @TableField("service_item_id")
    private Long serviceItemId;

    @TableField("created_time")
    private LocalDateTime createdTime;

}
