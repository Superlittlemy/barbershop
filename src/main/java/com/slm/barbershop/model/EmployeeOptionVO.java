package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 员工选项(轻量 VO)
 * <p>
 * 供会员门户预约弹窗等场景做下拉选择,只暴露必要字段。
 */
@Data
@Schema(description = "员工选项(下拉用)")
public class EmployeeOptionVO {

    @Schema(description = "员工ID")
    private Long id;

    @Schema(description = "工号")
    private String employeeNo;

    @Schema(description = "姓名")
    private String name;

}
