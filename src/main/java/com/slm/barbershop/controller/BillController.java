package com.slm.barbershop.controller;

import com.slm.barbershop.entity.Shop;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.model.ApiResponse;
import com.slm.barbershop.model.BillQuery;
import com.slm.barbershop.model.BillRequest;
import com.slm.barbershop.model.BillResponse;
import com.slm.barbershop.model.BillSummaryVO;
import com.slm.barbershop.model.PageResult;
import com.slm.barbershop.service.BillService;
import com.slm.barbershop.service.ShopService;
import com.slm.barbershop.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@Tag(name = "账单", description = "独立账单系统(支持非会员/会员/4 种支付方式/作废)")
@RestController
@RequestMapping("/bill")
public class BillController {

    @Autowired
    private BillService billService;

    @Autowired
    private ShopService shopService;

    @Operation(summary = "创建账单")
    @PostMapping
    public ApiResponse<BillResponse> create(@RequestBody @Valid BillRequest request) {
        assertShopAccess(request.getShopId());
        return ApiResponse.ok(billService.create(request));
    }

    @Operation(summary = "账单分页查询")
    @GetMapping("/page")
    public ApiResponse<PageResult<BillResponse>> page(BillQuery query) {
        assertShopAccess(query.getShopId());
        return ApiResponse.ok(billService.page(query));
    }

    @Operation(summary = "账单汇总(今日/本月/各支付方式)")
    @GetMapping("/summary")
    public ApiResponse<BillSummaryVO> summary(@RequestParam Long shopId) {
        assertShopAccess(shopId);
        return ApiResponse.ok(billService.summary(shopId));
    }

    @Operation(summary = "账单详情")
    @Parameter(name = "id", description = "账单ID", in = ParameterIn.PATH)
    @GetMapping("/{id}")
    public ApiResponse<BillResponse> detail(@PathVariable Long id, @RequestParam Long shopId) {
        assertShopAccess(shopId);
        return ApiResponse.ok(billService.getDetail(shopId, id));
    }

    @Operation(summary = "作废账单(MEMBER 时反向回退会员余额)")
    @Parameter(name = "id", description = "账单ID", in = ParameterIn.PATH)
    @PostMapping("/{id}/cancel")
    public ApiResponse<BillResponse> cancel(@PathVariable Long id,
                                            @RequestParam Long shopId,
                                            @RequestBody Map<String, String> body) {
        assertShopAccess(shopId);
        String reason = body == null ? null : body.get("reason");
        return ApiResponse.ok(billService.cancel(shopId, id, reason));
    }

    private void assertShopAccess(Long shopId) {
        if (shopId == null) {
            throw new BizException(HttpStatus.BAD_REQUEST, "shopId 不能为空");
        }
        Long userId = UserContext.getUser().getId();
        Shop shop = shopService.getById(shopId);
        if (shop == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "店铺不存在");
        }
        if (!shop.getUserId().equals(userId)) {
            throw new BizException(HttpStatus.FORBIDDEN, "无权限访问此店铺");
        }
    }

}
