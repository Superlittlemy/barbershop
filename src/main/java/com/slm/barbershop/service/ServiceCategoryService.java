package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.ServiceCategoryConverter;
import com.slm.barbershop.entity.ServiceCategory;
import com.slm.barbershop.mapper.ServiceCategoryMapper;
import com.slm.barbershop.model.ServiceCategoryRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ServiceCategoryService extends ServiceImpl<ServiceCategoryMapper, ServiceCategory> {

    @Autowired
    private ServiceCategoryMapper categoryMapper;

    @Autowired
    private ServiceCategoryConverter categoryConverter;

    public ServiceCategory create(ServiceCategoryRequest request) {
        ServiceCategory category = categoryConverter.toEntity(request);
        categoryMapper.insert(category);
        return category;
    }

    public void update(Long id, ServiceCategoryRequest request) {
        ServiceCategory category = new ServiceCategory();
        category.setId(id);
        categoryConverter.updateEntity(category, request);
        categoryMapper.updateById(category);
    }

    public Optional<ServiceCategory> get(Long id) {
        return this.lambdaQuery().eq(ServiceCategory::getId, id).oneOpt();
    }

    /**
     * 分页查询某店铺的分类
     * includeOff=false 时仅返回 status=1 的分类。
     */
    public IPage<ServiceCategory> page(IPage<ServiceCategory> page, Long shopId, boolean includeOff) {
        return categoryMapper.selectPage(page, new LambdaQueryWrapper<ServiceCategory>()
                .eq(ServiceCategory::getShopId, shopId)
                .eq(!includeOff, ServiceCategory::getStatus, 1));
    }

}
