package com.slm.barbershop.controller;

import com.slm.barbershop.entity.Shop;
import com.slm.barbershop.entity.ShopBusinessHours;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.model.ApiResponse;
import com.slm.barbershop.model.ShopBusinessHoursRequest;
import com.slm.barbershop.model.ShopBusinessHoursResponse;
import com.slm.barbershop.service.ShopBusinessHoursService;
import com.slm.barbershop.service.ShopService;
import com.slm.barbershop.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "店铺营业时间", description = "店铺营业时间管理相关接口")
@RestController
@RequestMapping("/shop/{id}/business-hours")
public class ShopBusinessHoursController {

    @Autowired
    private ShopBusinessHoursService businessHoursService;

    @Autowired
    private ShopService shopService;

    @Operation(summary = "查询店铺营业时间")
    @Parameter(name = "id", description = "店铺ID", in = ParameterIn.PATH)
    @GetMapping
    public ApiResponse<List<ShopBusinessHoursResponse>> list(@PathVariable Long id) {
        return ApiResponse.ok(businessHoursService.listByShopId(id).stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
    }

    @Operation(summary = "批量替换店铺营业时间(按 shopId 软删后全量插入)")
    @Parameter(name = "id", description = "店铺ID", in = ParameterIn.PATH)
    @PutMapping
    public ApiResponse<Void> replace(@PathVariable Long id,
                                     @RequestBody List<ShopBusinessHoursRequest> requests) {
        // 权限校验:仅店铺所属用户可改
        Long userId = UserContext.getUser() == null ? null : UserContext.getUser().getId();
        Shop shop = shopService.getById(id);
        if (shop == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "店铺不存在");
        }
        if (userId == null || !shop.getUserId().equals(userId)) {
            throw new BizException(HttpStatus.FORBIDDEN, "无权限修改此店铺的营业时间");
        }
        businessHoursService.replaceAll(id, requests);
        return ApiResponse.ok();
    }

    private ShopBusinessHoursResponse toResponse(ShopBusinessHours h) {
        ShopBusinessHoursResponse r = new ShopBusinessHoursResponse();
        r.setId(h.getId());
        r.setShopId(h.getShopId());
        r.setDayOfWeek(h.getDayOfWeek());
        r.setStartTime(h.getStartTime());
        r.setEndTime(h.getEndTime());
        r.setCrossDay(h.getCrossDay());
        r.setSortNo(h.getSortNo());
        return r;
    }

}
