package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.ShopConverter;
import com.slm.barbershop.entity.Shop;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.ShopMapper;
import com.slm.barbershop.model.PageResult;
import com.slm.barbershop.model.ShopOverviewStatsVO;
import com.slm.barbershop.model.ShopQuery;
import com.slm.barbershop.model.ShopRequest;
import com.slm.barbershop.model.ShopStatsQuery;
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

    public IPage<Shop> page(ShopQuery query, Long userId) {
        Page<Shop> page = new Page<>(query.getCurrent(), query.getSize());
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<Shop>()
                .eq(Shop::getUserId, userId);
        return shopMapper.selectPage(page, wrapper);
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

    public PageResult<ShopStatsVO> statsPage(ShopStatsQuery query, Long userId) {
        long p = Math.max(query.getCurrent(), 1);
        long s = Math.min(Math.max(query.getSize(), 1), 999);
        long offset = (p - 1L) * s;
        long total = shopMapper.countShopStats(userId);
        List<ShopStatsVO> records = shopMapper.selectShopStatsPage(userId, offset, s);
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
        Page<ShopStatsVO> result = new Page<>(p, s, total);
        result.setRecords(records);
        return PageResult.of(result);
    }

}