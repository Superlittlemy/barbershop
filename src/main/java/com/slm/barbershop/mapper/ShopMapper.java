package com.slm.barbershop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slm.barbershop.entity.Shop;
import com.slm.barbershop.model.ShopOverviewStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Mapper
public interface ShopMapper extends BaseMapper<Shop> {

    ShopOverviewStatsVO selectOverviewBase(@Param("userId") Long userId);

    BigDecimal sumRecentConsumeAmount(@Param("userId") Long userId,
                                      @Param("since") LocalDateTime since);

}