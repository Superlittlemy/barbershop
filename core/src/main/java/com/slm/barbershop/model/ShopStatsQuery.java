package com.slm.barbershop.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 店铺统计分页查询条件
 * <p>userId 由 controller 从 UserContext 取；排序由 mapper SQL 固定为 sortNo
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "店铺统计分页查询")
public class ShopStatsQuery extends Page<ShopStatsVO> {

}
