package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.slm.barbershop.entity.Bill;
import com.slm.barbershop.model.BillPageVO;
import com.slm.barbershop.model.BillQuery;
import com.slm.barbershop.model.BillRequest;
import com.slm.barbershop.model.BillResponse;
import com.slm.barbershop.model.BillSummaryVO;
import com.slm.barbershop.model.ShopBillRecentVO;

import java.util.List;

public interface BillService {

    /**
     * 创建账单(MEMBER 时原子扣会员余额,失败整体回滚)
     */
    BillResponse create(BillRequest request);

    /**
     * 作废账单(MEMBER 时反向回退会员余额)
     */
    BillResponse cancel(Long shopId, Long billId, String reason);

    /**
     * 分页查询账单(含 items)
     */
    BillPageVO page(Long shopId, IPage<Bill> page, BillQuery query);

    /**
     * 店铺账单汇总(今日/本月/各支付方式)
     */
    BillSummaryVO summary(Long shopId);

    /**
     * 账单详情(含 items)
     */
    BillResponse getDetail(Long shopId, Long billId);

    /**
     * 店铺最近 N 条账单
     */
    List<ShopBillRecentVO> listRecentByShopId(Long shopId, int limit);

}
