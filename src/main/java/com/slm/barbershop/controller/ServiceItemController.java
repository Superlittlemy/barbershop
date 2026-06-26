package com.slm.barbershop.controller;

import com.slm.barbershop.entity.ServiceItem;
import com.slm.barbershop.model.ApiResponse;
import com.slm.barbershop.model.ServiceItemRequest;
import com.slm.barbershop.model.ServiceItemResponse;
import com.slm.barbershop.service.ServiceItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "服务项", description = "消费项目管理相关接口")
@RestController
@RequestMapping("/service-item")
public class ServiceItemController {

    @Autowired
    private ServiceItemService itemService;

    @Operation(summary = "创建消费项目")
    @PostMapping
    public ApiResponse<ServiceItemResponse> create(@RequestBody ServiceItemRequest request) {
        ServiceItem item = itemService.create(request);
        return ApiResponse.ok(itemService.listByShopId(item.getShopId(), null, true).stream()
                .filter(r -> r.getId().equals(item.getId()))
                .findFirst()
                .orElse(null));
    }

    @Operation(summary = "更新消费项目")
    @Parameter(name = "id", description = "项目ID", in = ParameterIn.PATH)
    @PutMapping("/{id}")
    public ApiResponse<ServiceItemResponse> update(@PathVariable Long id,
                                                   @RequestBody ServiceItemRequest request) {
        itemService.update(id, request);
        return ApiResponse.ok(itemService.listByShopId(request.getShopId(), null, true).stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null));
    }

    @Operation(summary = "删除消费项目")
    @Parameter(name = "id", description = "项目ID", in = ParameterIn.PATH)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, @RequestParam Long shopId) {
        itemService.delete(shopId, id);
        return ApiResponse.ok();
    }

    @Operation(summary = "获取消费项目详情")
    @Parameter(name = "id", description = "项目ID", in = ParameterIn.PATH)
    @GetMapping("/{id}")
    public ApiResponse<ServiceItemResponse> getById(@PathVariable Long id, @RequestParam Long shopId) {
        return ApiResponse.ok(itemService.listByShopId(shopId, null, true).stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null));
    }

    @Operation(summary = "列出店铺消费项目")
    @GetMapping("/list")
    public ApiResponse<List<ServiceItemResponse>> list(@RequestParam Long shopId,
                                                       @RequestParam(required = false) Long categoryId,
                                                       @RequestParam(defaultValue = "false") boolean includeOff) {
        return ApiResponse.ok(itemService.listByShopId(shopId, categoryId, includeOff));
    }

}
