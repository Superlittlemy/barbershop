package com.slm.barbershop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 会员实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("member")
public class Member extends BaseEntity {

    /**
     * 所属店铺ID
     */
    private Long shopId;

    /**
     * 会员姓名
     */
    private String name;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 余额
     */
    private BigDecimal balance;

}