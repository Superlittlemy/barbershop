package com.slm.barbershop.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.slm.barbershop.entity.Employee;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 员工分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "员工分页查询条件(含分页)")
public class EmployeeQuery extends Page<Employee> {

    @NotNull(message = "店铺ID不能为空")
    @Schema(description = "店铺ID(必填)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long shopId;

    @Schema(description = "关键字(模糊匹配姓名或工号,可选)")
    private String keyword;

}
