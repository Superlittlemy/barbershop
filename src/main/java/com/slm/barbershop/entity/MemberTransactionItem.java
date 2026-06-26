package com.slm.barbershop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会员交易-消费项目明细
 *
 * <p>作为 {@link MemberTransaction} 的子实体,记录一笔消费包含的项目及数量。
 * 关键字段(itemName / unitPrice)为冗余快照,
 * 在服务项改名/调价/下架后历史交易记录仍保持原样。
 */
@Data
@TableName("member_transaction_item")
public class MemberTransactionItem {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联交易ID
     */
    @TableField("transaction_id")
    private Long transactionId;

    /**
     * 关联项目ID
     */
    @TableField("item_id")
    private Long itemId;

    /**
     * 项目名(下单时冗余快照)
     */
    @TableField("item_name")
    private String itemName;

    /**
     * 下单时单价(冗余快照)
     */
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /**
     * 数量
     */
    private Integer quantity;

    /**
     * 小计
     */
    private BigDecimal subtotal;

    /**
     * 排序号
     */
    @TableField("sort_no")
    private Integer sortNo;

    /**
     * 创建时间
     */
    @TableField("created_time")
    private LocalDateTime createdTime;

}
