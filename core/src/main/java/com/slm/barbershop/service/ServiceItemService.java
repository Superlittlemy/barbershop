package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.ServiceItemConverter;
import com.slm.barbershop.entity.ServiceCategory;
import com.slm.barbershop.entity.ServiceItem;
import com.slm.barbershop.mapper.ServiceItemMapper;
import com.slm.common.model.PageResult;
import com.slm.barbershop.model.ServiceItemQuery;
import com.slm.barbershop.model.ServiceItemRequest;
import com.slm.barbershop.model.ServiceItemResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    public PageResult<ServiceItemResponse> page(ServiceItemQuery query) {
        LambdaQueryWrapper<ServiceItem> wrapper = new LambdaQueryWrapper<ServiceItem>()
                .eq(ServiceItem::getShopId, query.getShopId())
                .eq(query.getCategoryId() != null, ServiceItem::getCategoryId, query.getCategoryId())
                .eq(!query.isIncludeOff(), ServiceItem::getStatus, 1);
        IPage<ServiceItem> itemPage = itemMapper.selectPage(query, wrapper);

        // 收集本页所有非空 categoryId,批量查分类名(一次 IN 查询)
        Set<Long> categoryIds = itemPage.getRecords().stream()
                .map(ServiceItem::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> nameMap = categoryIds.isEmpty() ? Collections.emptyMap()
                : categoryService.listByIds(categoryIds).stream()
                    .collect(Collectors.toMap(ServiceCategory::getId, ServiceCategory::getName));

        List<ServiceItemResponse> mapped = itemPage.getRecords().stream()
                .map(item -> {
                    ServiceItemResponse resp = itemConverter.toResponse(item);
                    if (item.getCategoryId() != null) {
                        resp.setCategoryName(nameMap.get(item.getCategoryId()));
                    }
                    return resp;
                })
                .collect(Collectors.toList());
        return PageResult.of(itemPage, mapped);
    }

}
