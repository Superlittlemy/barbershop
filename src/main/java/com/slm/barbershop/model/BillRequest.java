package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 账单创建请求
 */
@Data
@Schema(description = "账单创建请求")
public class BillRequest {

    @NotNull
    @Schema(description = "店铺ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long shopId;

    @Schema(description = "会员ID(支付方式为 MEMBER 时必填)")
    private Long memberId;

    @Size(max = 50)
    @Schema(description = "客户姓名(非会员时必填)")
    private String customerName;

    @Size(max = 20)
    @Schema(description = "客户手机号(非会员可选)")
    private String customerPhone;

    @NotBlank
    @Schema(description = "支付方式:MEMBER / OFFLINE / WECHAT / ALIPAY", requiredMode = Schema.RequiredMode.REQUIRED)
    private String payChannel;

    @Schema(description = "备注")
    private String remark;

    @NotEmpty
    @Schema(description = "服务项目明细", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<TransactionItemRequest> items;

    @Size(max = 64)
    @Schema(description = "幂等键(客户端UUID;为空时由服务端生成)")
    private String idempotencyKey;

}
