package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.ShopConverter;
import com.slm.barbershop.entity.Shop;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.ShopMapper;
import com.slm.barbershop.model.ShopOverviewStatsVO;
import com.slm.barbershop.model.ShopRequest;
import com.slm.barbershop.model.ShopStatsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShopService extends ServiceImpl<ShopMapper, Shop> {

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private ShopConverter shopConverter;

    public Shop create(ShopRequest request, Long userId) {
        Shop shop = shopConverter.toEntityWithUserId(request, userId);
        shopMapper.insert(shop);
        return shop;
    }

    public Shop update(Long id, ShopRequest request, Long userId) {
        Shop shop = getById(id);
        if (shop == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "店铺不存在");
        }
        if (!shop.getUserId().equals(userId)) {
            throw new BizException(HttpStatus.FORBIDDEN, "无权限修改此店铺");
        }
        shopConverter.updateEntity(shop, request);
        shopMapper.updateById(shop);
        return shop;
    }

    public void delete(Long id, Long userId) {
        Shop shop = getById(id);
        if (shop == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "店铺不存在");
        }
        if (!shop.getUserId().equals(userId)) {
            throw new BizException(HttpStatus.FORBIDDEN, "无权限删除此店铺");
        }
        shopMapper.deleteById(id);
    }

    public IPage<Shop> page(IPage<Shop> page, Long userId) {
        return this.lambdaQuery().eq(Shop::getUserId, userId).page(page);
    }

    public ShopOverviewStatsVO overviewStats(Long userId) {
        LocalDateTime asOf = LocalDateTime.now();
        LocalDateTime since = asOf.minusDays(30);
        ShopOverviewStatsVO stats = shopMapper.selectOverviewBase(userId);
        BigDecimal recentTxAmount = shopMapper.sumRecentConsumeAmount(userId, since);
        stats.setTotalBalance(stats.getTotalBalance().setScale(2, RoundingMode.HALF_UP));
        stats.setRecentTxAmount(recentTxAmount.setScale(2, RoundingMode.HALF_UP));
        stats.setAsOf(asOf);
        return stats;
    }

    public IPage<ShopStatsVO> statsPage(IPage<ShopStatsVO> page, Long userId) {
        long total = shopMapper.countShopStats(userId);
        long current = page.getCurrent() <= 0 ? 1L : page.getCurrent();
        long size = page.getSize() <= 0 ? 999L : page.getSize();
        long offset = (current - 1L) * size;
        List<ShopStatsVO> records = shopMapper.selectShopStatsPage(userId, offset, size);
        if (records != null) {
            for (ShopStatsVO record : records) {
                BigDecimal balance = record.getTotalBalance() == null
                        ? BigDecimal.ZERO : record.getTotalBalance();
                record.setTotalBalance(balance.setScale(2, RoundingMode.HALF_UP));
                if (record.getMemberCount() == null) {
                    record.setMemberCount(0);
                }
            }
        }
        IPage<ShopStatsVO> result = new Page<>(current, size, total);
        result.setRecords(records);
        return result;
    }

}