package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 店铺响应
 */
@Data
@Schema(description = "店铺响应")
public class ShopResponse {

    @Schema(description = "店铺ID")
    private Long id;

    @Schema(description = "店铺名称")
    private String name;

    @Schema(description = "店铺Logo URL")
    private String logo;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "营业开始时间(HH:mm:ss)")
    private LocalTime openTime;

    @Schema(description = "营业结束时间(HH:mm:ss)")
    private LocalTime closeTime;

    @Schema(description = "周内休息标记,7 元素 0/1 数组,索引 0=周一 ... 6=周日")
    private List<Integer> weeklyOff;

    @Schema(description = "所属用户ID")
    private Long userId;

    @Schema(description = "注册时间")
    private LocalDateTime createdTime;

}
