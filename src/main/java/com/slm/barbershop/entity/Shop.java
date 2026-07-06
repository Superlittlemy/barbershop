package com.slm.barbershop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 店铺实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop")
public class Shop extends BaseEntity {

    /**
     * 店铺名称
     */
    private String name;

    /**
     * 店铺Logo
     */
    private String logo;

    /**
     * 地址
     */
    private String address;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 营业时间
     */
    private String businessHours;

    /**
     * 所属用户ID
     */
    private Long userId;

}