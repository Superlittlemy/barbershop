package com.slm.barbershop.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.slm.barbershop.entity.ServiceItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 服务项分页查询条件
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "服务项分页查询")
public class ServiceItemQuery extends Page<ServiceItem> {

    @NotNull(message = "店铺ID不能为空")
    @Schema(description = "店铺ID(必填)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long shopId;

    @Schema(description = "服务项分类ID(可选,按分类筛选)")
    private Long categoryId;

    @Schema(description = "是否包括下架项目(默认 false)")
    private Boolean includeOff;

    public boolean isIncludeOff() {
        return includeOff != null && includeOff;
    }

}
