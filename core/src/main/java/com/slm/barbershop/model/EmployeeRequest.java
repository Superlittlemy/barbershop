package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * 员工创建/更新请求
 * <p>
 * 工号由系统生成,入参不包含 employeeNo;更新时 shopId 亦不可改。
 */
@Data
@Schema(description = "员工创建/更新请求")
public class EmployeeRequest {

    @NotNull(message = "所属店铺ID不能为空")
    @Schema(description = "所属店铺ID(更新时不可改)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long shopId;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50)
    @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Min(value = 1, message = "性别取值无效")
    @Max(value = 2, message = "性别取值无效")
    @Schema(description = "性别:1.男 2.女(可空)")
    private Integer gender;

    @Schema(description = "入职时间(yyyy-MM-dd,可空)")
    private LocalDate hireDate;

    @Schema(description = "可提供的服务项目ID列表(多选,可空)")
    private List<Long> serviceItemIds;

}
