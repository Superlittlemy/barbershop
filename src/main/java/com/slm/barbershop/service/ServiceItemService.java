package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.ServiceItemConverter;
import com.slm.barbershop.entity.MemberTransactionItem;
import com.slm.barbershop.entity.ServiceCategory;
import com.slm.barbershop.entity.ServiceItem;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.MemberTransactionItemMapper;
import com.slm.barbershop.mapper.ServiceCategoryMapper;
import com.slm.barbershop.mapper.ServiceItemMapper;
import com.slm.barbershop.model.ServiceItemRequest;
import com.slm.barbershop.model.ServiceItemResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
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
    private ServiceCategoryMapper categoryMapper;

    @Autowired
    private MemberTransactionItemMapper transactionItemMapper;

    public ServiceItem create(ServiceItemRequest request) {
        validate(request);
        ServiceItem item = new ServiceItem();
        item.setShopId(request.getShopId());
        item.setCategoryId(request.getCategoryId());
        item.setName(request.getName());
        item.setPrice(request.getPrice());
        item.setDescription(request.getDescription());
        item.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        itemMapper.insert(item);
        return item;
    }

    public ServiceItem update(Long id, ServiceItemRequest request) {
        ServiceItem item = getByIdAndShopId(request.getShopId(), id);
        if (item == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "消费项目不存在");
        }
        if (request.getPrice() != null) {
            item.setPrice(request.getPrice());
        }
        itemConverter.updateEntity(item, request);
        itemMapper.updateById(item);
        return item;
    }

    public void delete(Long shopId, Long id) {
        ServiceItem item = getByIdAndShopId(shopId, id);
        if (item == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "消费项目不存在");
        }
        // 已被历史交易引用则禁止删除(冗余快照允许历史保留,但禁止破坏性删除)
        Long refCount = transactionItemMapper.selectCount(
                new LambdaQueryWrapper<MemberTransactionItem>()
                        .eq(MemberTransactionItem::getItemId, id)
        );
        if (refCount != null && refCount > 0) {
            throw new BizException(HttpStatus.CONFLICT,
                    "项目已被历史消费引用,无法删除(可改为下架状态)");
        }
        itemMapper.deleteById(id);
    }

    public ServiceItem getById(Long shopId, Long id) {
        return getByIdAndShopId(shopId, id);
    }

    /**
     * 列出某店铺的项目。
     * includeOff=false 时仅返回 status=1 的项目;
     * categoryId 非空时按分类筛选。
     */
    public List<ServiceItemResponse> listByShopId(Long shopId, Long categoryId, boolean includeOff) {
        if (shopId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ServiceItem> wrapper = new LambdaQueryWrapper<ServiceItem>()
                .eq(ServiceItem::getShopId, shopId)
                .orderByAsc(ServiceItem::getCategoryId)
                .orderByDesc(ServiceItem::getId);
        if (categoryId != null) {
            wrapper.eq(ServiceItem::getCategoryId, categoryId);
        }
        if (!includeOff) {
            wrapper.eq(ServiceItem::getStatus, 1);
        }
        List<ServiceItem> list = itemMapper.selectList(wrapper);
        List<ServiceItemResponse> responses = itemConverter.toResponseList(list);
        fillCategoryName(responses, list);
        return responses;
    }

    /**
     * 批量查询项目(用于消费时按 ID 集合校验/读取单价/名称)。
     * 严格限定 shopId,防止跨店使用。
     */
    public List<ServiceItem> listByIdsAndShopId(Long shopId, Set<Long> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyList();
        }
        return itemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getShopId, shopId)
                        .in(ServiceItem::getId, itemIds)
        );
    }

    private void fillCategoryName(List<ServiceItemResponse> responses, List<ServiceItem> items) {
        if (responses == null || responses.isEmpty()) {
            return;
        }
        Set<Long> categoryIds = items.stream()
                .map(ServiceItem::getCategoryId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return;
        }
        List<ServiceCategory> categories = categoryMapper.selectBatchIds(categoryIds);
        Map<Long, String> nameMap = categories.stream()
                .collect(Collectors.toMap(ServiceCategory::getId, ServiceCategory::getName, (a, b) -> a));
        responses.forEach(r -> {
            if (r.getCategoryId() != null) {
                r.setCategoryName(nameMap.get(r.getCategoryId()));
            }
        });
    }

    private ServiceItem getByIdAndShopId(Long shopId, Long id) {
        return itemMapper.selectOne(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getShopId, shopId)
                        .eq(ServiceItem::getId, id)
        );
    }

    private void validate(ServiceItemRequest request) {
        if (request.getShopId() == null) {
            throw new BizException(HttpStatus.BAD_REQUEST, "所属店铺ID不能为空");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BizException(HttpStatus.BAD_REQUEST, "项目名称不能为空");
        }
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(HttpStatus.BAD_REQUEST, "项目单价必须大于等于0");
        }
    }

}
