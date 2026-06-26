package com.slm.barbershop.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 消费项目实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_item")
public class ServiceItem extends BaseEntity {

    /**
     * 所属店铺ID
     */
    private Long shopId;

    /**
     * 所属分类ID(可空:不分类的项目)
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 项目名称
     */
    private String name;

    /**
     * 默认单价
     */
    private BigDecimal price;

    /**
     * 项目描述
     */
    private String description;

    /**
     * 状态:0.下架 1.上架
     */
    private Integer status;

}
