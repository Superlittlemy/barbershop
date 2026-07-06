package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

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

    @Schema(description = "营业时间")
    private String businessHours;

}