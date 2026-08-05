package com.slm.barbershop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.slm.barbershop.converter.ServiceCategoryConverter;
import com.slm.barbershop.entity.ServiceCategory;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.mapper.ServiceCategoryMapper;
import com.slm.barbershop.model.ServiceCategoryQuery;
import com.slm.barbershop.model.ServiceCategoryRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ServiceCategoryService extends ServiceImpl<ServiceCategoryMapper, ServiceCategory> {

    @Autowired
    private ServiceCategoryMapper categoryMapper;

    @Autowired
    private ServiceCategoryConverter categoryConverter;

    public ServiceCategory create(ServiceCategoryRequest request) {
        ServiceCategory category = categoryConverter.toEntity(request);
        // 自动生成 sortNo = 当前店铺最大 sortNo + 步长(无记录则为步长)
        category.setSortNo(nextSortNo(category.getShopId()));
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
     * <p>
     * 业务硬约束：分类必须按 sort_no 升序排序，**不接受前端 sort 参数覆盖**（page.orders.clear()）
     * includeOff=false 时仅返回 status=1 的分类。
     */
    public IPage<ServiceCategory> page(ServiceCategoryQuery query) {
        Page<ServiceCategory> page = new Page<>(query.getCurrent(), query.getSize());
        page.orders().clear();
        page.orders().add(OrderItem.asc("sort_no"));
        return categoryMapper.selectPage(page, new LambdaQueryWrapper<ServiceCategory>()
                .eq(ServiceCategory::getShopId, query.getShopId())
                .eq(!query.isIncludeOff(), ServiceCategory::getStatus, 1));
    }

    /**
     * 计算下一个可用 sortNo。
     */
    private int nextSortNo(Long shopId) {
        ServiceCategory max = this.lambdaQuery()
                .eq(ServiceCategory::getShopId, shopId)
                .orderByDesc(ServiceCategory::getSortNo)
                .last("LIMIT 1")
                .one();
        return max == null || max.getSortNo() == null ? 0 : max.getSortNo();
    }

    /**
     * 按新顺序批量重排某店铺的所有分类。
     * orderedIds 中的每个 id 必须属于该店铺,否则抛 400。
     * 按位置依次赋 sortNo = (i+1) * 步长,保证顺序确定、留出插入空隙。
     */
    public void reorder(Long shopId, List<Long> orderedIds) {
        if (shopId == null) {
            throw new BizException(HttpStatus.BAD_REQUEST, "店铺ID不能为空");
        }
        if (orderedIds == null || orderedIds.isEmpty()) {
            return;
        }
        // 去重
        Set<Long> unique = new HashSet<>(orderedIds);
        if (unique.size() != orderedIds.size()) {
            throw new BizException(HttpStatus.BAD_REQUEST, "排序ID列表存在重复");
        }
        // 校验所有 id 都属于该店铺
        Long matched = categoryMapper.selectCount(new LambdaQueryWrapper<ServiceCategory>()
                .eq(ServiceCategory::getShopId, shopId)
                .in(ServiceCategory::getId, orderedIds));
        if (matched == null || matched.intValue() != orderedIds.size()) {
            throw new BizException(HttpStatus.BAD_REQUEST, "排序ID列表包含非本店铺的分类");
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            ServiceCategory patch = new ServiceCategory();
            patch.setId(orderedIds.get(i));
            patch.setSortNo(i + 1);
            categoryMapper.updateById(patch);
        }
    }

}
