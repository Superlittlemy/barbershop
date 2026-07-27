package com.slm.barbershop.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账单主实体(与 member_transaction 物理隔离)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bill")
public class Bill extends BaseEntity {

    /**
     * 所属店铺ID
     */
    @TableField("shop_id")
    private Long shopId;

    /**
     * 会员ID(可空:非会员场景)
     */
    @TableField("member_id")
    private Long memberId;

    /**
     * 客户姓名(非会员快照)
     */
    @TableField("customer_name")
    private String customerName;

    /**
     * 客户手机号(非会员快照)
     */
    @TableField("customer_phone")
    private String customerPhone;

    /**
     * 支付方式
     */
    @TableField("pay_channel")
    private String payChannel;

    /**
     * 账单类型:CONSUME-消费 STORE-储值
     */
    @TableField("type")
    private String type;

    /**
     * 账单总金额
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否作废:0.正常 1.已作废
     */
    @TableField("is_cancelled")
    private Integer isCancelled;

    /**
     * 作废时间
     */
    @TableField("cancelled_time")
    private LocalDateTime cancelledTime;

    /**
     * 作废操作人
     */
    @TableField("cancelled_by")
    private Long cancelledBy;

    /**
     * 作废原因
     */
    @TableField("cancel_reason")
    private String cancelReason;

    /**
     * 幂等键(客户端UUID)
     */
    @TableField("idempotency_key")
    private String idempotencyKey;

    /**
     * 乐观锁版本号(基类未声明,这里补 @Version)
     */
    @Version
    @TableField("version")
    private Long version;

}
