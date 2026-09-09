package com.slm.barbershop.controller;

import com.slm.barbershop.entity.Employee;
import com.slm.barbershop.entity.Shop;
import com.slm.common.enums.ResultStatus;
import com.slm.common.exception.BizException;
import com.slm.barbershop.model.EmployeeOptionVO;
import com.slm.barbershop.model.EmployeeQuery;
import com.slm.barbershop.model.EmployeeRequest;
import com.slm.barbershop.model.EmployeeResponse;
import com.slm.common.model.ApiResponse;
import com.slm.common.model.PageResult;
import com.slm.barbershop.service.EmployeeService;
import com.slm.barbershop.service.ShopService;
import com.slm.common.context.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "员工", description = "店铺员工维护(工号自动生成/服务项目多选;账单与预约可关联员工)")
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private ShopService shopService;

    @Operation(summary = "创建员工")
    @PostMapping
    public ApiResponse<EmployeeResponse> create(@RequestBody @Valid EmployeeRequest request) {
        assertShopAccess(request.getShopId());
        Employee employee = employeeService.create(request);
        return ApiResponse.success(employeeService.toResponseWithItems(employee));
    }

    @Operation(summary = "更新员工(店铺与工号不可改)")
    @Parameter(name = "id", description = "员工ID", in = ParameterIn.PATH)
    @PutMapping("/{id}")
    public ApiResponse<EmployeeResponse> update(@PathVariable Long id,
                                                @RequestBody EmployeeRequest request) {
        assertShopAccess(loadEmployeeShopId(id));
        Employee employee = employeeService.update(id, request);
        return ApiResponse.success(employeeService.toResponseWithItems(employee));
    }

    @Operation(summary = "删除员工")
    @Parameter(name = "id", description = "员工ID", in = ParameterIn.PATH)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        assertShopAccess(loadEmployeeShopId(id));
        employeeService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "员工详情")
    @Parameter(name = "id", description = "员工ID", in = ParameterIn.PATH)
    @GetMapping("/{id}")
    public ApiResponse<EmployeeResponse> detail(@PathVariable Long id, @RequestParam Long shopId) {
        assertShopAccess(shopId);
        return ApiResponse.success(employeeService.getDetail(shopId, id));
    }

    @Operation(summary = "员工分页查询")
    @Parameter(name = "shopId", description = "店铺ID", in = ParameterIn.QUERY, required = true)
    @Parameter(name = "keyword", description = "关键字(模糊匹配姓名或工号)", in = ParameterIn.QUERY)
    @GetMapping("/page")
    public ApiResponse<PageResult<EmployeeResponse>> page(@Validated EmployeeQuery query) {
        assertShopAccess(query.getShopId());
        return ApiResponse.success(employeeService.page(query));
    }

    /**
     * 门户预约下拉用:登录即可访问(店家或会员身份),仅校验 shopId 非空,
     * 口径对齐 /appointment/available-slots。
     */
    @Operation(summary = "员工选项列表(门户预约下拉用,登录即可)")
    @Parameter(name = "shopId", description = "店铺ID", in = ParameterIn.QUERY, required = true)
    @GetMapping("/options")
    public ApiResponse<List<EmployeeOptionVO>> options(@RequestParam Long shopId) {
        return ApiResponse.success(employeeService.listOptions(shopId));
    }

    private Long loadEmployeeShopId(Long id) {
        Employee employee = employeeService.getOptById(id)
                .orElseThrow(() -> new BizException(ResultStatus.SOURCE_NOT_FOUND, "员工不存在"));
        return employee.getShopId();
    }

    private void assertShopAccess(Long shopId) {
        if (shopId == null) {
            throw new BizException(ResultStatus.BAD_REQUEST, "shopId 不能为空");
        }
        Long userId = UserContext.getUser().getId();
        Shop shop = shopService.getById(shopId);
        if (shop == null) {
            throw new BizException(ResultStatus.SOURCE_NOT_FOUND, "店铺不存在");
        }
        if (!shop.getUserId().equals(userId)) {
            throw new BizException(ResultStatus.PERMISSION_DENIED, "无权限访问此店铺");
        }
    }

}
