package com.slm.barbershop.controller;

import com.slm.barbershop.converter.MemberConverter;
import com.slm.barbershop.converter.MemberTransactionConverter;
import com.slm.barbershop.entity.Member;
import com.slm.barbershop.entity.MemberTransaction;
import com.slm.barbershop.model.ApiResponse;
import com.slm.barbershop.model.MemberRequest;
import com.slm.barbershop.model.MemberResponse;
import com.slm.barbershop.model.MemberTransactionRequest;
import com.slm.barbershop.model.MemberTransactionResponse;
import com.slm.barbershop.service.MemberService;
import com.slm.barbershop.service.MemberTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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

    @Operation(summary = "创建会员")
    @PostMapping
    public ApiResponse<MemberResponse> create(@RequestBody MemberRequest request) {
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
    public ApiResponse<Void> delete(@PathVariable Long id, @RequestParam Long shopId) {
        memberService.delete(shopId, id);
        return ApiResponse.ok();
    }

    @Operation(summary = "获取会员")
    @Parameter(name = "id", description = "会员ID", in = ParameterIn.PATH)
    @GetMapping("/{id}")
    public ApiResponse<MemberResponse> getById(@PathVariable Long id, @RequestParam Long shopId) {
        Member member = memberService.getById(shopId, id);
        return ApiResponse.ok(memberConverter.toResponse(member));
    }

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
        MemberTransaction transaction = transactionService.consume(id, request.getAmount(), request.getRemark(), request.getIdempotencyKey());
        return ApiResponse.ok(transactionConverter.toResponse(transaction));
    }

    @Operation(summary = "查询余额")
    @Parameter(name = "id", description = "会员ID", in = ParameterIn.PATH)
    @GetMapping("/{id}/balance")
    public ApiResponse<BigDecimal> getBalance(@PathVariable Long id) {
        BigDecimal balance = transactionService.getBalance(id);
        return ApiResponse.ok(balance);
    }

    @Operation(summary = "交易流水")
    @Parameter(name = "id", description = "会员ID", in = ParameterIn.PATH)
    @GetMapping("/{id}/transactions")
    public ApiResponse<List<MemberTransactionResponse>> transactions(@PathVariable Long id) {
        List<MemberTransaction> transactions = transactionService.listByMemberId(id);
        return ApiResponse.ok(transactionConverter.toResponseList(transactions));
    }

}