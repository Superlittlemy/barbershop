package com.slm.barbershop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.slm.barbershop.converter.ShopConverter;
import com.slm.barbershop.entity.Shop;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.model.ApiResponse;
import com.slm.barbershop.model.ShopOverviewStatsVO;
import com.slm.barbershop.model.ShopRequest;
import com.slm.barbershop.model.ShopResponse;
import com.slm.barbershop.model.ShopStatsVO;
import com.slm.barbershop.model.ShopTransactionRecentVO;
import com.slm.barbershop.service.MemberTransactionService;
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

@Tag(name = "店铺", description = "店铺管理相关接口")
@RestController
@RequestMapping("/shop")
public class ShopController {

    @Autowired
    private ShopService shopService;

    @Autowired
    private ShopConverter shopConverter;

    @Autowired
    private MemberTransactionService memberTransactionService;

    @Operation(summary = "创建店铺")
    @PostMapping
    public ApiResponse<ShopResponse> create(@RequestBody ShopRequest request) {
        Long userId = UserContext.getUser().getId();
        Shop shop = shopService.create(request, userId);
        return ApiResponse.ok(shopConverter.toResponse(shop));
    }

    @Operation(summary = "更新店铺")
    @Parameter(name = "id", description = "店铺ID", in = ParameterIn.PATH)
    @PutMapping("/{id}")
    public ApiResponse<ShopResponse> update(@PathVariable Long id, @RequestBody ShopRequest request) {
        Long userId = UserContext.getUser().getId();
        Shop shop = shopService.update(id, request, userId);
        return ApiResponse.ok(shopConverter.toResponse(shop));
    }

    @Operation(summary = "删除店铺")
    @Parameter(name = "id", description = "店铺ID", in = ParameterIn.PATH)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getUser().getId();
        shopService.delete(id, userId);
        return ApiResponse.ok();
    }

    @Operation(summary = "获取店铺")
    @Parameter(name = "id", description = "店铺ID", in = ParameterIn.PATH)
    @GetMapping("/{id}")
    public ApiResponse<ShopResponse> getById(@PathVariable Long id) {
        Shop shop = shopService.getById(id);
        return ApiResponse.ok(shopConverter.toResponse(shop));
    }

    @Operation(summary = "获取用户店铺分页列表")
    @GetMapping("/page")
    public ApiResponse<IPage<ShopResponse>> page(IPage<Shop> page) {
        Long userId = UserContext.getUser().getId();
        return ApiResponse.ok(shopService.page(page, userId)
                .convert(shopConverter::toResponse));
    }

    @Operation(summary = "获取店铺最近交易记录(右侧最新动态)")
    @Parameter(name = "id", description = "店铺ID", in = ParameterIn.PATH)
    @GetMapping("/{id}/transactions/recent")
    public ApiResponse<List<ShopTransactionRecentVO>> recentTransactions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "8") int limit) {
        Long userId = UserContext.getUser().getId();
        Shop shop = shopService.getById(id);
        if (shop == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "店铺不存在");
        }
        if (!shop.getUserId().equals(userId)) {
            throw new BizException(HttpStatus.FORBIDDEN, "无权限查看此店铺");
        }
        return ApiResponse.ok(memberTransactionService.listRecentByShopId(id, limit));
    }

    @Operation(summary = "获取概览页 KPI 聚合数据")
    @GetMapping("/stats/overview")
    public ApiResponse<ShopOverviewStatsVO> statsOverview() {
        Long userId = UserContext.getUser().getId();
        return ApiResponse.ok(shopService.overviewStats(userId));
    }

    @Operation(summary = "获取店铺列表（含每店会员聚合：memberCount、totalBalance）")
    @GetMapping("/stats/list")
    public ApiResponse<IPage<ShopStatsVO>> statsList(IPage<ShopStatsVO> page) {
        Long userId = UserContext.getUser().getId();
        return ApiResponse.ok(shopService.statsPage(page, userId));
    }

}