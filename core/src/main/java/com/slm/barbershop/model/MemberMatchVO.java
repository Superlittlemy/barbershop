package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会员跨店铺匹配结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "会员跨店铺匹配结果")
public class MemberMatchVO {

    @Schema(description = "会员ID")
    private Long memberId;

    @Schema(description = "所属店铺ID")
    private Long shopId;

    @Schema(description = "店铺名称")
    private String shopName;

    @Schema(description = "会员姓名")
    private String name;

    @Schema(description = "手机号")
    private String phone;

}
