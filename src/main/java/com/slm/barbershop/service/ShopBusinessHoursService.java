package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.entity.ShopBusinessHours;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.ShopBusinessHoursMapper;
import com.slm.barbershop.model.ShopBusinessHoursRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 店铺营业时间
 * <p>
 * 写入策略:按 shopId 软删后批量新增(简单可靠,避免部分失败导致数据不一致);
 * 营业时间是会员预约时段的唯一依据,写入后即时生效。
 */
@Service
public class ShopBusinessHoursService extends ServiceImpl<ShopBusinessHoursMapper, ShopBusinessHours> {

    @Autowired
    private ShopBusinessHoursMapper businessHoursMapper;

    /**
     * 列出某店铺的所有营业时间(按 dayOfWeek, sortNo 排序)。
     */
    public List<ShopBusinessHours> listByShopId(Long shopId) {
        return businessHoursMapper.selectList(new LambdaQueryWrapper<ShopBusinessHours>()
                .eq(ShopBusinessHours::getShopId, shopId)
                .orderByAsc(ShopBusinessHours::getDayOfWeek)
                .orderByAsc(ShopBusinessHours::getSortNo));
    }

    /**
     * 批量替换某店铺的营业时间:先软删全部,再批量插入。
     * <p>
     * 入参校验:起始 < 结束(非跨日);start/end 都不为空(由 @NotNull 兜底)。
     */
    public void replaceAll(Long shopId, List<ShopBusinessHoursRequest> requests) {
        if (shopId == null) {
            throw new BizException(HttpStatus.BAD_REQUEST, "店铺ID不能为空");
        }
        if (requests != null) {
            for (ShopBusinessHoursRequest r : requests) {
                if (Boolean.TRUE.equals(notCrossDay(r)) && !r.getEndTime().isAfter(r.getStartTime())) {
                    throw new BizException(HttpStatus.BAD_REQUEST, "非跨日营业时间必须 startTime < endTime");
                }
                if (Boolean.TRUE.equals(isCrossDay(r)) && !r.getEndTime().isBefore(r.getStartTime())) {
                    throw new BizException(HttpStatus.BAD_REQUEST, "跨日营业时间必须 endTime < startTime");
                }
            }
        }
        // 1) 软删当前店铺全部营业时间
        List<ShopBusinessHours> existing = listByShopId(shopId);
        for (ShopBusinessHours h : existing) {
            businessHoursMapper.deleteById(h.getId());
        }
        // 2) 批量插入
        if (requests == null || requests.isEmpty()) {
            return;
        }
        List<ShopBusinessHours> entities = requests.stream().map(r -> {
            ShopBusinessHours h = new ShopBusinessHours();
            h.setShopId(shopId);
            h.setDayOfWeek(r.getDayOfWeek());
            h.setStartTime(r.getStartTime());
            h.setEndTime(r.getEndTime());
            h.setCrossDay(Boolean.TRUE.equals(isCrossDay(r)) ? 1 : 0);
            h.setSortNo(r.getSortNo() == null ? 0 : r.getSortNo());
            return h;
        }).collect(Collectors.toList());
        for (ShopBusinessHours h : entities) {
            businessHoursMapper.insert(h);
        }
    }

    private Boolean notCrossDay(ShopBusinessHoursRequest r) {
        return !Boolean.TRUE.equals(isCrossDay(r));
    }

    private Boolean isCrossDay(ShopBusinessHoursRequest r) {
        return r.getCrossDay() != null && r.getCrossDay() == 1;
    }

}
