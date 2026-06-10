package com.slm.barbershop.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.ShopConverter;
import com.slm.barbershop.entity.Shop;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.ShopMapper;
import com.slm.barbershop.model.ShopRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

    public List<Shop> listByUserId(Long userId) {
        return this.lambdaQuery().eq(Shop::getUserId, userId).orderByDesc(Shop::getId).list();
    }

}