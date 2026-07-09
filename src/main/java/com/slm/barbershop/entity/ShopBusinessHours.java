package com.slm.barbershop.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalTime;

/**
 * 店铺营业时间
 * <p>
 * 按"周内几号 + 起止 + 是否跨日"独立成表,作为会员预约时段的唯一依据。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shop_business_hours")
public class ShopBusinessHours extends BaseEntity {

    /**
     * 所属店铺ID
     */
    private Long shopId;

    /**
     * 周内几号:1=周一 ... 7=周日
     */
    private Integer dayOfWeek;

    /**
     * 起始时间(HH:mm:ss)
     */
    private LocalTime startTime;

    /**
     * 结束时间(HH:mm:ss)
     */
    private LocalTime endTime;

    /**
     * 是否跨日:1=跨日(end_time<start_time)
     */
    private Integer crossDay;

    /**
     * 排序号
     */
    private Integer sortNo;

}
