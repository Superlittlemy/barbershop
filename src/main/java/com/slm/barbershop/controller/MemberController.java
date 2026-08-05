package com.slm.barbershop.controller;

import com.slm.barbershop.converter.MemberConverter;
import com.slm.barbershop.converter.MemberTransactionConverter;
import com.slm.barbershop.entity.Member;
import com.slm.barbershop.entity.MemberTransaction;
import com.slm.barbershop.exception.BizException;
import com.slm.barbershop.model.ApiResponse;
import com.slm.barbershop.model.LoginResponse;
import com.slm.barbershop.model.MemberLoginRequest;
import com.slm.barbershop.model.MemberMatchVO;
import com.slm.barbershop.model.MemberQuery;
import com.slm.barbershop.model.MemberRequest;
import com.slm.barbershop.model.MemberResponse;
import com.slm.barbershop.model.MemberTransactionQuery;
import com.slm.barbershop.model.MemberTransactionRequest;
import com.slm.barbershop.model.MemberTransactionResponse;
import com.slm.barbershop.model.PageResult;
import com.slm.barbershop.service.MemberAuthService;
import com.slm.barbershop.service.MemberService;
import com.slm.barbershop.service.MemberTransactionService;
import com.slm.barbershop.utils.UserContext;
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

@Tag(name = "会员", description = "会员管理相关接口")
@RestController
@RequestMapping("/member")
public class MemberController {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberConverter memberConverter;

    @Autowired
    private MemberTransactionService transactionService;

    @Autowired
    private MemberTransactionConverter transactionConverter;

    @Autowired
    private MemberAuthService memberAuthService;

    @Operation(summary = "创建会员")
    @PostMapping
    public ApiResponse<MemberResponse> create(@RequestBody @Valid MemberRequest request) {
        Member member = memberService.create(request.getShopId(), request);
        return ApiResponse.ok(memberConverter.toResponse(member));
    }

    @Operation(summary = "更新会员")
    @Parameter(name = "id", description = "会员ID", in = ParameterIn.PATH)
    @PutMapping("/{id}")
    public ApiResponse<MemberResponse> update(@PathVariable Long id, @RequestBody MemberRequest request) {
        Member member = memberService.update(request.getShopId(), id, request);
        return ApiResponse.ok(memberConverter.toResponse(member));
    }

    @Operation(summary = "删除会员")
    @Parameter(name = "id", description = "会员ID", in = ParameterIn.PATH)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        memberService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "获取会员")
    @Parameter(name = "id", description = "会员ID", in = ParameterIn.PATH)
    @GetMapping("/{id}")
    public ApiResponse<MemberResponse> getById(@PathVariable Long id, @RequestParam Long shopId) {
        Member member = memberService.getByIdAndShopId(shopId, id).orElseThrow(() -> new BizException(HttpStatus.NOT_FOUND, "会员不存在"));
        return ApiResponse.ok(memberConverter.toResponse(member));
    }

    @Operation(summary = "获取店铺会员列表")
    @GetMapping("/page")
    public ApiResponse<PageResult<MemberResponse>> page(@Validated MemberQuery query) {
        return ApiResponse.ok(PageResult.map(memberService.page(query), memberConverter::toResponse));
    }

    @Deprecated
    @Operation(summary = "获取店铺会员列表")
    @GetMapping("/list")
    public ApiResponse<List<MemberResponse>> list(@RequestParam Long shopId) {
        List<Member> members = memberService.listByShopId(shopId);
        return ApiResponse.ok(memberConverter.toResponseList(members));
    }

    @Operation(summary = "储值")
    @Parameter(name = "id", description = "会员ID", in = ParameterIn.PATH)
    @PostMapping("/{id}/store")
    public ApiResponse<MemberTransactionResponse> store(@PathVariable Long id, @RequestBody MemberTransactionRequest request) {
        MemberTransaction transaction = transactionService.store(id, request.getAmount(), request.getRemark(), request.getIdempotencyKey());
        return ApiResponse.ok(transactionConverter.toResponse(transaction));
    }

    @Operation(summary = "消费")
    @Parameter(name = "id", description = "会员ID", in = ParameterIn.PATH)
    @PostMapping("/{id}/consume")
    public ApiResponse<MemberTransactionResponse> consume(@PathVariable Long id, @RequestBody MemberTransactionRequest request) {
        MemberTransaction transaction = transactionService.consume(id, request.getAmount(), request.getRemark(), request.getItems(), request.getIdempotencyKey());
        // 回填 items 字段(根据 id 查询明细)
        MemberTransactionResponse response = transactionConverter.toResponse(transaction);
        response.setItems(transactionService.listItemsByTransactionId(transaction.getId()));
        return ApiResponse.ok(response);
    }

    @Operation(summary = "交易流水")
    @Parameter(name = "id", description = "会员ID", in = ParameterIn.PATH)
    @GetMapping("/{id}/transactions")
    public ApiResponse<List<MemberTransactionResponse>> transactions(@PathVariable Long id) {
        assertMemberSelf(id);
        return ApiResponse.ok(transactionService.listByMemberId(id));
    }

    // ====== 新增：会员门户相关接口 ======

    @Operation(summary = "跨店铺查找会员(按手机号+姓名)")
    @GetMapping("/login/match")
    public ApiResponse<List<MemberMatchVO>> matchForLogin(
            @RequestParam String phone,
            @RequestParam String name) {
        if (phone == null || phone.trim().isEmpty() || name == null || name.trim().isEmpty()) {
            throw new BizException(HttpStatus.BAD_REQUEST, "手机号和姓名不能为空");
        }
        return ApiResponse.ok(memberService.match(phone, name));
    }

    @Operation(summary = "会员登录(返回JWT)")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> loginForMember(@RequestBody MemberLoginRequest request) {
        LoginResponse response = memberAuthService.login(
                request.getPhone(), request.getName(), request.getShopId());
        return ApiResponse.ok(response);
    }

    @Operation(summary = "会员交易流水分页查询")
    @Parameter(name = "id", description = "会员ID", in = ParameterIn.PATH)
    @GetMapping("/{id}/transactions/page")
    public ApiResponse<PageResult<MemberTransactionResponse>> transactionsPage(
            @PathVariable Long id, MemberTransactionQuery query) {
        assertMemberSelf(id);
        return ApiResponse.ok(transactionService.pageByMemberId(id, query));
    }

    /**
     * 校验当前请求是否来自会员本人。
     * 仅会员身份的 token 且 memberId 与 path 中 {id} 一致时通过。
     * 后台用户（管理员）调用同样放行,便于管理员代查。
     */
    private void assertMemberSelf(Long pathMemberId) {
        var user = UserContext.getUser();
        if (user == null) {
            throw new BizException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        if (user.isMember() && !user.getId().equals(pathMemberId)) {
            throw new BizException(HttpStatus.FORBIDDEN, "无权访问该会员数据");
        }
    }

}
