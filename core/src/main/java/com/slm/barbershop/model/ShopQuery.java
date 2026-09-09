package com.slm.barbershop.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.slm.barbershop.entity.Shop;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 店铺分页查询条件
 * <p>userId 由 controller 从 UserContext 取，不进 query
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "店铺分页查询")
public class ShopQuery extends Page<Shop> {

}
