package com.slm.barbershop.controller;

import com.slm.barbershop.converter.ShopConverter;
import com.slm.barbershop.entity.Shop;
import com.slm.barbershop.model.ShopRequest;
import com.slm.barbershop.model.ShopResponse;
import com.slm.barbershop.service.ShopService;
import com.slm.barbershop.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "店铺", description = "店铺管理相关接口")
@RestController
@RequestMapping("/shop")
public class ShopController {

    @Autowired
    private ShopService shopService;

    @Autowired
    private ShopConverter shopConverter;

    @Operation(summary = "创建店铺")
    @PostMapping
    public ShopResponse create(@RequestBody ShopRequest request) {
        Long userId = UserContext.getUser().getId();
        Shop shop = shopService.create(request, userId);
        return shopConverter.toResponse(shop);
    }

    @Operation(summary = "更新店铺")
    @Parameter(name = "id", description = "店铺ID", in = ParameterIn.PATH)
    @PutMapping("/{id}")
    public ShopResponse update(@PathVariable Long id, @RequestBody ShopRequest request) {
        Long userId = UserContext.getUser().getId();
        Shop shop = shopService.update(id, request, userId);
        return shopConverter.toResponse(shop);
    }

    @Operation(summary = "删除店铺")
    @Parameter(name = "id", description = "店铺ID", in = ParameterIn.PATH)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        Long userId = UserContext.getUser().getId();
        shopService.delete(id, userId);
    }

    @Operation(summary = "获取店铺")
    @Parameter(name = "id", description = "店铺ID", in = ParameterIn.PATH)
    @GetMapping("/{id}")
    public ShopResponse getById(@PathVariable Long id) {
        Shop shop = shopService.getById(id);
        return shopConverter.toResponse(shop);
    }

    @Operation(summary = "获取用户店铺列表")
    @GetMapping("/list")
    public List<ShopResponse> list() {
        Long userId = UserContext.getUser().getId();
        List<Shop> shops = shopService.listByUserId(userId);
        return shopConverter.toResponseList(shops);
    }

}