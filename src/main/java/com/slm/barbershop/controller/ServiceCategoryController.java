package com.slm.barbershop.controller;

import com.slm.barbershop.entity.ServiceCategory;
import com.slm.barbershop.model.ApiResponse;
import com.slm.barbershop.model.ServiceCategoryRequest;
import com.slm.barbershop.model.ServiceCategoryResponse;
import com.slm.barbershop.service.ServiceCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "服务项分类", description = "消费项目分类管理相关接口")
@RestController
@RequestMapping("/service-category")
public class ServiceCategoryController {

    @Autowired
    private ServiceCategoryService categoryService;

    @Operation(summary = "创建分类")
    @PostMapping
    public ApiResponse<ServiceCategoryResponse> create(@RequestBody ServiceCategoryRequest request) {
        ServiceCategory category = categoryService.create(request);
        return ApiResponse.ok(categoryService.listByShopId(category.getShopId(), true).stream()
                .filter(r -> r.getId().equals(category.getId()))
                .findFirst()
                .orElseGet(() -> toSingleResponse(category)));
    }

    @Operation(summary = "更新分类")
    @Parameter(name = "id", description = "分类ID", in = ParameterIn.PATH)
    @PutMapping("/{id}")
    public ApiResponse<ServiceCategoryResponse> update(@PathVariable Long id,
                                                       @RequestBody ServiceCategoryRequest request) {
        ServiceCategory category = categoryService.update(id, request);
        return ApiResponse.ok(toSingleResponse(category));
    }

    @Operation(summary = "删除分类")
    @Parameter(name = "id", description = "分类ID", in = ParameterIn.PATH)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @RequestParam Long shopId) {
        categoryService.delete(shopId, id);
        return ApiResponse.ok();
    }

    @Operation(summary = "获取分类详情")
    @Parameter(name = "id", description = "分类ID", in = ParameterIn.PATH)
    @GetMapping("/{id}")
    public ApiResponse<ServiceCategoryResponse> getById(@PathVariable Long id, @RequestParam Long shopId) {
        ServiceCategory category = categoryService.getById(shopId, id);
        if (category == null) {
            return ApiResponse.ok(null);
        }
        return ApiResponse.ok(toSingleResponse(category));
    }

    @Operation(summary = "列出店铺分类")
    @GetMapping("/list")
    public ApiResponse<List<ServiceCategoryResponse>> list(@RequestParam Long shopId,
                                                           @RequestParam(defaultValue = "false") boolean includeOff) {
        return ApiResponse.ok(categoryService.listByShopId(shopId, includeOff));
    }

    private ServiceCategoryResponse toSingleResponse(ServiceCategory category) {
        ServiceCategoryResponse response = new ServiceCategoryResponse();
        response.setId(category.getId());
        response.setShopId(category.getShopId());
        response.setName(category.getName());
        response.setSortNo(category.getSortNo());
        response.setStatus(category.getStatus());
        response.setCreatedTime(category.getCreatedTime());
        response.setUpdatedTime(category.getUpdatedTime());
        response.setItemCount(0L);
        return response;
    }

}
