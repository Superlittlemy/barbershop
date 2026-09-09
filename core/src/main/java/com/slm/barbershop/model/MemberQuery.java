package com.slm.barbershop.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.slm.barbershop.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 会员分页查询条件（分页+查询合一）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "会员分页查询")
public class MemberQuery extends Page<Member> {

    @NotNull(message = "店铺ID不能为空")
    @Schema(description = "店铺ID(必填,用于数据隔离)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long shopId;

    @Schema(description = "会员名称/手机号")
    private String keyword;

}
