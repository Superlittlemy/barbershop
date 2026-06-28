package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.ServiceItemConverter;
import com.slm.barbershop.entity.ServiceItem;
import com.slm.barbershop.mapper.ServiceItemMapper;
import com.slm.barbershop.model.ServiceItemRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServiceItemService extends ServiceImpl<ServiceItemMapper, ServiceItem> {

    @Autowired
    private ServiceItemMapper itemMapper;

    @Autowired
    private ServiceItemConverter itemConverter;

    public ServiceItem create(ServiceItemRequest request) {
        ServiceItem item = itemConverter.toEntity(request);
        itemMapper.insert(item);
        return item;
    }

    public void update(Long id, ServiceItemRequest request) {
        ServiceItem item = new ServiceItem();
        item.setId(id);
        itemConverter.updateEntity(item, request);
        itemMapper.updateById(item);
    }

    /**
     * 分页查询某店铺的项目。
     * includeOff=false 时仅返回 status=1 的项目;
     * categoryId 非空时按分类筛选。
     */
    public IPage<ServiceItem> page(IPage<ServiceItem> page, Long shopId, Long categoryId, boolean includeOff) {
        return itemMapper.selectPage(page, new LambdaQueryWrapper<ServiceItem>()
                .eq(ServiceItem::getShopId, shopId)
                .eq(categoryId != null, ServiceItem::getCategoryId, categoryId)
                .eq(!includeOff, ServiceItem::getStatus, 1));
    }

}
