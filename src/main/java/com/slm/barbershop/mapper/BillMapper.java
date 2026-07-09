package com.slm.barbershop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slm.barbershop.entity.Bill;
import com.slm.barbershop.model.BillSummaryRowVO;
import com.slm.barbershop.model.ShopBillRecentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BillMapper extends BaseMapper<Bill> {

    /**
     * 一次性聚合店铺维度的今日/本月/各支付方式金额与笔数。
     * 通过 CASE WHEN 分桶 + GROUP BY pay_channel 实现一次扫描。
     */
    List<BillSummaryRowVO> summarizeByShopId(@Param("shopId") Long shopId,
                                             @Param("todayStart") LocalDateTime todayStart,
                                             @Param("monthStart") LocalDateTime monthStart);

    /**
     * 店铺最近 N 条账单(LEFT JOIN member 取会员名)
     */
    List<ShopBillRecentVO> listRecentByShopId(@Param("shopId") Long shopId,
                                               @Param("limit") int limit);

}
