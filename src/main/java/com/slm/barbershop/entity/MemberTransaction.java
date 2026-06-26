package com.slm.barbershop.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 会员交易流水实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member_transaction")
public class MemberTransaction extends BaseEntity {

    /**
     * 会员ID
     */
    private Long memberId;

    /**
     * 交易类型
     */
    private String type;

    /**
     * 交易金额
     */
    private BigDecimal amount;

    /**
     * 交易前余额
     */
    private BigDecimal balanceBefore;

    /**
     * 交易后余额
     */
    private BigDecimal balanceAfter;

    /**
     * 备注
     */
    private String remark;

    /**
     * 幂等键(客户端UUID)
     */
    @TableField("idempotency_key")
    private String idempotencyKey;

}
