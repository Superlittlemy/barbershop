package com.slm.barbershop.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账单明细
 * <p>itemName / unitPrice 为冗余快照,
 * 服务项改名/调价/下架后历史账单仍保持原样。
 */
@Data
@TableName("bill_item")
public class BillItem {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("bill_id")
    private Long billId;

    @TableField("item_id")
    private Long itemId;

    @TableField("item_name")
    private String itemName;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal subtotal;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("created_time")
    private LocalDateTime createdTime;

}
