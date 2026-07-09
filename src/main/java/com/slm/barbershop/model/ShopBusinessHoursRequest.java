package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalTime;

/**
 * 店铺营业时间请求
 */
@Data
@Schema(description = "店铺营业时间请求")
public class ShopBusinessHoursRequest {

    @NotNull(message = "周内几号不能为空")
    @Min(value = 1, message = "周内几号必须在 1-7 之间")
    @Max(value = 7, message = "周内几号必须在 1-7 之间")
    @Schema(description = "周内几号:1=周一 ... 7=周日")
    private Integer dayOfWeek;

    @NotNull(message = "起始时间不能为空")
    @Schema(description = "起始时间(HH:mm:ss)")
    private LocalTime startTime;

    @NotNull(message = "结束时间不能为空")
    @Schema(description = "结束时间(HH:mm:ss)")
    private LocalTime endTime;

    @Schema(description = "是否跨日:1=跨日(end_time<start_time),0=不跨日")
    private Integer crossDay;

    @Schema(description = "排序号")
    private Integer sortNo;

}
