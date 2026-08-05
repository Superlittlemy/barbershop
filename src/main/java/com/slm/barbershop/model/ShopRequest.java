package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalTime;
import java.util.List;

/**
 * 店铺请求
 */
@Data
@Schema(description = "店铺请求")
public class ShopRequest {

    @Schema(description = "店铺名称")
    private String name;

    @Schema(description = "店铺Logo URL")
    private String logo;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "营业开始时间(HH:mm:ss,默认 09:00:00)")
    private LocalTime openTime;

    @Schema(description = "营业结束时间(HH:mm:ss,默认 22:00:00;必须 >= openTime)")
    private LocalTime closeTime;

    @Schema(description = "周内休息标记,7 元素 0/1 数组,索引 0=周一 ... 6=周日;如 [1,0,0,0,0,0,0] 表示周一休息")
    private List<Integer> weeklyOff;

}
