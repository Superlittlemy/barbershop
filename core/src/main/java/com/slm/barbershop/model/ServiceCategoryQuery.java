package com.slm.barbershop.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.slm.barbershop.entity.ServiceCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 服务项分类分页查询条件
 * <p>业务上分类必须按 sort_no 排序，不接受前端 sort 参数覆盖（service 内强制）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "服务项分类分页查询")
public class ServiceCategoryQuery extends Page<ServiceCategory> {

    @NotNull(message = "店铺ID不能为空")
    @Schema(description = "店铺ID(必填)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long shopId;

    @Schema(description = "是否包括下架分类(默认 false)")
    private Boolean includeOff;

    public boolean isIncludeOff() {
        return includeOff != null && includeOff;
    }

}
