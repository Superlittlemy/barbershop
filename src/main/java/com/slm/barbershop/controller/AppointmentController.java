package com.slm.barbershop.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.slm.barbershop.entity.Appointment;
import com.slm.barbershop.entity.Shop;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.model.ApiResponse;
import com.slm.barbershop.model.AppointmentRequest;
import com.slm.barbershop.model.AppointmentResponse;
import com.slm.barbershop.model.AuthUser;
import com.slm.barbershop.service.AppointmentService;
import com.slm.barbershop.service.ShopService;
import com.slm.barbershop.utils.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "预约", description = "会员预约相关接口")
@RestController
@RequestMapping("/appointment")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private ShopService shopService;

    @Operation(summary = "创建预约(会员)")
    @PostMapping
    public ApiResponse<AppointmentResponse> create(@RequestBody @Valid AppointmentRequest request) {
        Appointment ap = appointmentService.create(request);
        return ApiResponse.ok(appointmentService.toResponseWithNames(ap));
    }

    @Operation(summary = "店家分页查询预约")
    @GetMapping("/page")
    public ApiResponse<IPage<AppointmentResponse>> page(
            @RequestParam Long shopId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String keyword,
            IPage<Appointment> page) {
        assertShopOwner(shopId);
        return ApiResponse.ok(appointmentService.pageByShop(page, shopId, date, keyword));
    }

    @Operation(summary = "会员端拉取可用时段(HH:mm 列表)")
    @GetMapping("/available-slots")
    public ApiResponse<List<String>> availableSlots(
            @RequestParam Long shopId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(appointmentService.listAvailableSlots(shopId, date));
    }

    private void assertShopOwner(Long shopId) {
        AuthUser auth = UserContext.getUser();
        if (auth == null) {
            throw new BizException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        if (auth.isMember()) {
            throw new BizException(HttpStatus.FORBIDDEN, "仅店家可查询");
        }
        Shop shop = shopService.getById(shopId);
        if (shop == null) {
            throw new BizException(HttpStatus.NOT_FOUND, "店铺不存在");
        }
        if (!shop.getUserId().equals(auth.getId())) {
            throw new BizException(HttpStatus.FORBIDDEN, "无权限查看此店铺");
        }
    }

}
