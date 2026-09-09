package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工响应
 */
@Data
@Schema(description = "员工响应")
public class EmployeeResponse {

    @Schema(description = "员工ID")
    private Long id;

    @Schema(description = "所属店铺ID")
    private Long shopId;

    @Schema(description = "工号")
    private String employeeNo;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "性别:1.男 2.女")
    private Integer gender;

    @Schema(description = "入职时间(yyyy-MM-dd)")
    private LocalDate hireDate;

    @Schema(description = "可提供的服务项目ID列表")
    private List<Long> serviceItemIds;

    @Schema(description = "可提供的服务项目名称列表(实时查询)")
    private List<String> serviceItemNames;

    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

}
