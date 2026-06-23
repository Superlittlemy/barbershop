package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

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

    @Schema(description = "所属用户ID")
    private Long userId;

    @Schema(description = "注册时间")
    private LocalDateTime createdTime;

}