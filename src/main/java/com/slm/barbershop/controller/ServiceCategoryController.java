package com.slm.barbershop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.slm.barbershop.converter.ServiceCategoryConverter;
import com.slm.barbershop.entity.ServiceCategory;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.model.ApiResponse;
import com.slm.barbershop.model.ServiceCategoryRequest;
import com.slm.barbershop.model.ServiceCategoryResponse;
import com.slm.barbershop.service.ServiceCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "服务项分类", description = "消费项目分类管理相关接口")
@RestController
@RequestMapping("/service-category")
public class ServiceCategoryController {

    @Autowired
    private ServiceCategoryService categoryService;

    @Autowired
    private ServiceCategoryConverter categoryConverter;

    @Operation(summary = "创建分类")
    @PostMapping
    public ApiResponse<ServiceCategoryResponse> create(@RequestBody @Valid ServiceCategoryRequest request) {
        ServiceCategory category = categoryService.create(request);
        return ApiResponse.ok(categoryConverter.toResponse(category));
    }

    @Operation(summary = "更新分类")
    @Parameter(name = "id", description = "分类ID", in = ParameterIn.PATH)
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                    @RequestBody ServiceCategoryRequest request) {
        categoryService.update(id, request);
        return ApiResponse.ok();
    }

    @Operation(summary = "删除分类")
    @Parameter(name = "id", description = "分类ID", in = ParameterIn.PATH)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.removeById(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "获取分类详情")
    @Parameter(name = "id", description = "分类ID", in = ParameterIn.PATH)
    @GetMapping("/{id}")
    public ApiResponse<ServiceCategoryResponse> getById(@PathVariable Long id) {
        ServiceCategory category = categoryService.getOptById(id).orElseThrow(() -> new BizException(HttpStatus.NOT_FOUND, "分类不存在"));
        return ApiResponse.ok(categoryConverter.toResponse(category));
    }

    @Operation(summary = "店铺分类分页")
    @GetMapping("/page")
    public ApiResponse<IPage<ServiceCategoryResponse>> page(@RequestParam Long shopId,
                                                            @RequestParam(defaultValue = "false") boolean includeOff,
                                                            IPage<ServiceCategory> page) {
        return ApiResponse.ok(categoryService.page(page, shopId, includeOff)
                .convert(categoryConverter::toResponse));
    }

}
