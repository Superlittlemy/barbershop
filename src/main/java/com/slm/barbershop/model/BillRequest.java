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
 * <p>
 * 非会员场景不再要求客户姓名/手机号(由"客户"列展示会员姓名快照即可),
 * 故创建入参不再包含 customerName / customerPhone 字段。
 */
@Data
@Schema(description = "账单创建请求")
public class BillRequest {

    @NotNull
    @Schema(description = "店铺ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long shopId;

    @Schema(description = "会员ID(支付方式为 MEMBER 时必填)")
    private Long memberId;

    @NotBlank
    @Schema(description = "支付方式:MEMBER / OFFLINE / WECHAT / ALIPAY", requiredMode = Schema.RequiredMode.REQUIRED)
    private String payChannel;

    @Schema(description = "账单类型:CONSUME(消费,默认) / STORE(储值)")
    private String type;

    @Schema(description = "备注")
    private String remark;

    @NotEmpty
    @Schema(description = "服务项目明细", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<TransactionItemRequest> items;

    @Size(max = 64)
    @Schema(description = "幂等键(客户端UUID;为空时由服务端生成)")
    private String idempotencyKey;

}