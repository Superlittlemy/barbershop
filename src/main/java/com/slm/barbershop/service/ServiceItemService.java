package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.ServiceItemConverter;
import com.slm.barbershop.entity.ServiceCategory;
import com.slm.barbershop.entity.ServiceItem;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.ServiceItemMapper;
import com.slm.barbershop.model.ServiceItemRequest;
import com.slm.barbershop.model.ServiceItemResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ServiceItemService extends ServiceImpl<ServiceItemMapper, ServiceItem> {

    @Autowired
    private ServiceItemMapper itemMapper;

    @Autowired
    private ServiceItemConverter itemConverter;

    @Autowired
    private ServiceCategoryService categoryService;

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
     * 分页查询某店铺的项目,并在 service 层完成 categoryName 的批量填充,
     * 避免 controller 关心跨实体的字段组装,顺带避免 N+1 查询。
     * <p>
     * includeOff=false 时仅返回 status=1 的项目;
     * categoryId 非空时按分类筛选。
     */
    public IPage<ServiceItemResponse> page(IPage<ServiceItem> page, Long shopId, Long categoryId, boolean includeOff) {
        IPage<ServiceItem> itemPage = itemMapper.selectPage(page, new LambdaQueryWrapper<ServiceItem>()
                .eq(ServiceItem::getShopId, shopId)
                .eq(categoryId != null, ServiceItem::getCategoryId, categoryId)
                .eq(!includeOff, ServiceItem::getStatus, 1));
        // 收集本页所有非空 categoryId,批量查分类名(一次 IN 查询)
        Set<Long> categoryIds = itemPage.getRecords().stream()
                .map(ServiceItem::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> nameMap = categoryIds.isEmpty() ? Collections.emptyMap()
                : categoryService.listByIds(categoryIds).stream()
                    .collect(Collectors.toMap(ServiceCategory::getId, ServiceCategory::getName));
        return itemPage.convert(item -> {
            ServiceItemResponse resp = itemConverter.toResponse(item);
            if (item.getCategoryId() != null) {
                resp.setCategoryName(nameMap.get(item.getCategoryId()));
            }
            return resp;
        });
    }

}
