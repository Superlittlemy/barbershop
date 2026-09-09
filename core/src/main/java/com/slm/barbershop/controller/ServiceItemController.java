package com.slm.barbershop.controller;

import com.slm.barbershop.converter.ServiceItemConverter;
import com.slm.barbershop.entity.ServiceItem;
import com.slm.common.enums.ResultStatus;
import com.slm.common.exception.BizException;
import com.slm.common.model.ApiResponse;
import com.slm.common.model.PageResult;
import com.slm.barbershop.model.ServiceItemQuery;
import com.slm.barbershop.model.ServiceItemRequest;
import com.slm.barbershop.model.ServiceItemResponse;
import com.slm.barbershop.service.ServiceItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = "服务项", description = "消费项目管理相关接口")
@RestController
@RequestMapping("/service-item")
public class ServiceItemController {

    @Autowired
    private ServiceItemService itemService;

    @Autowired
    private ServiceItemConverter itemConverter;

    @Operation(summary = "创建消费项目")
    @PostMapping
    public ApiResponse<ServiceItemResponse> create(@RequestBody @Valid ServiceItemRequest request) {
        ServiceItem item = itemService.create(request);
        return ApiResponse.success(itemConverter.toResponse(item));
    }

    @Operation(summary = "更新消费项目")
    @Parameter(name = "id", description = "项目ID", in = ParameterIn.PATH)
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id,
                                   @RequestBody ServiceItemRequest request) {
        itemService.update(id, request);
        return ApiResponse.success();
    }

    @Operation(summary = "删除消费项目")
    @Parameter(name = "id", description = "项目ID", in = ParameterIn.PATH)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        itemService.removeById(id);
        return ApiResponse.success();
    }

    @Operation(summary = "获取消费项目详情")
    @Parameter(name = "id", description = "项目ID", in = ParameterIn.PATH)
    @GetMapping("/{id}")
    public ApiResponse<ServiceItemResponse> getById(@PathVariable Long id) {
        ServiceItem serviceItem = itemService.getOptById(id).orElseThrow(() -> new BizException(ResultStatus.SOURCE_NOT_FOUND, "消费项目不存在"));
        return ApiResponse.success(itemConverter.toResponse(serviceItem));
    }

    @Operation(summary = "店铺消费项目分页")
    @Parameter(name = "shopId", description = "店铺ID", in = ParameterIn.QUERY, required = true)
    @Parameter(name = "categoryId", description = "服务项分类ID", in = ParameterIn.QUERY)
    @Parameter(name = "includeOff", description = "是否包括下架", in = ParameterIn.QUERY)
    @GetMapping("/page")
    public ApiResponse<PageResult<ServiceItemResponse>> page(@Validated ServiceItemQuery query) {
        return ApiResponse.success(itemService.page(query));
    }

}
