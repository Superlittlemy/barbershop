package com.slm.barbershop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slm.barbershop.entity.MemberTransaction;
import com.slm.barbershop.model.ShopTransactionRecentVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberTransactionMapper extends BaseMapper<MemberTransaction> {

    /**
     * 按店铺查询最近 N 条交易记录(JOIN member 取会员姓名)。
     * 注意:member_transaction 表有 is_deleted 字段,自定义 SQL 需手动过滤。
     */
    List<ShopTransactionRecentVO> listRecentByShopId(@Param("shopId") Long shopId,
                                                     @Param("limit") int limit);

}