package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.ServiceCategoryConverter;
import com.slm.barbershop.entity.ServiceCategory;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.ServiceCategoryMapper;
import com.slm.barbershop.model.ServiceCategoryRequest;
import com.slm.barbershop.model.ServiceCategoryResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ServiceCategoryService extends ServiceImpl<ServiceCategoryMapper, ServiceCategory> {

    @Autowired
    private ServiceCategoryMapper categoryMapper;

    @Autowired
    private ServiceCategoryConverter categoryConverter;

    public ServiceCategory create(ServiceCategoryRequest request) {
        validateShop(request.getShopId());
        ServiceCategory category = new ServiceCategory();
        category.setShopId(request.getShopId());
        category.setName(request.getName());
        category.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        category.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        categoryMapper.insert(category);
        return category;
    }

    public ServiceCategory update(Long id, ServiceCategoryRequest request) {
        ServiceCategory category = getByIdAndShopId(request.getShopId(), id);
        if (category == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "分类不存在");
        }
        categoryConverter.updateEntity(category, request);
        categoryMapper.updateById(category);
        return category;
    }

    public void delete(Long shopId, Long id) {
        ServiceCategory category = getByIdAndShopId(shopId, id);
        if (category == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "分类不存在");
        }
        categoryMapper.deleteById(id);
    }

    public ServiceCategory getById(Long shopId, Long id) {
        return getByIdAndShopId(shopId, id);
    }

    /**
     * 列出某店铺的分类。
     * includeOff=false 时仅返回 status=1 的分类。
     */
    public List<ServiceCategoryResponse> listByShopId(Long shopId, boolean includeOff) {
        if (shopId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ServiceCategory> wrapper = new LambdaQueryWrapper<ServiceCategory>()
                .eq(ServiceCategory::getShopId, shopId)
                .orderByAsc(ServiceCategory::getSortNo)
                .orderByDesc(ServiceCategory::getId);
        if (!includeOff) {
            wrapper.eq(ServiceCategory::getStatus, 1);
        }
        List<ServiceCategory> list = categoryMapper.selectList(wrapper);
        List<ServiceCategoryResponse> responses = categoryConverter.toResponseList(list);
        responses.forEach(r -> r.setItemCount(0L));
        return responses;
    }

    /**
     * 批量回填每个分类下的项目数量。
     * 供其他 Service 在列表场景中调用。
     */
    public void fillItemCount(List<ServiceCategoryResponse> responses,
                              Map<Long, Long> categoryIdToCount) {
        if (responses == null || responses.isEmpty()) {
            return;
        }
        responses.forEach(r -> {
            Long count = categoryIdToCount.get(r.getId());
            r.setItemCount(count == null ? 0L : count);
        });
    }

    private ServiceCategory getByIdAndShopId(Long shopId, Long id) {
        return categoryMapper.selectOne(
                new LambdaQueryWrapper<ServiceCategory>()
                        .eq(ServiceCategory::getShopId, shopId)
                        .eq(ServiceCategory::getId, id)
        );
    }

    private void validateShop(Long shopId) {
        if (shopId == null) {
            throw new BizException(HttpStatus.BAD_REQUEST, "所属店铺ID不能为空");
        }
    }

}
