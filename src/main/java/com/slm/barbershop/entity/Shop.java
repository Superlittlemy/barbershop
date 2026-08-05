package com.slm.barbershop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

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
     * 营业开始时间(HH:mm:ss)
     */
    private LocalTime openTime;

    /**
     * 营业结束时间(HH:mm:ss,必须 >= openTime)
     */
    private LocalTime closeTime;

    /**
     * 周内休息日标记:7 位 0/1,索引 0=周一 ... 6=周日,'1'=休
     */
    private String weeklyOff;

    /**
     * 所属用户ID
     */
    private Long userId;

}