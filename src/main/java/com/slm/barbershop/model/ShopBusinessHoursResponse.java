package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalTime;

/**
 * 店铺营业时间响应
 */
@Data
@Schema(description = "店铺营业时间响应")
public class ShopBusinessHoursResponse {

    @Schema(description = "营业时间ID")
    private Long id;

    @Schema(description = "所属店铺ID")
    private Long shopId;

    @Schema(description = "周内几号:1=周一 ... 7=周日")
    private Integer dayOfWeek;

    @Schema(description = "起始时间(HH:mm:ss)")
    private LocalTime startTime;

    @Schema(description = "结束时间(HH:mm:ss)")
    private LocalTime endTime;

    @Schema(description = "是否跨日:1=跨日 0=不跨日")
    private Integer crossDay;

    @Schema(description = "排序号")
    private Integer sortNo;

}
