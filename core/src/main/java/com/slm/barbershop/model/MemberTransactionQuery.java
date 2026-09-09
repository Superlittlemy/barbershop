package com.slm.barbershop.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.slm.barbershop.entity.MemberTransaction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会员交易流水分页查询条件
 * <p>memberId 走 URL path 变量,不进 query
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "会员交易分页查询")
public class MemberTransactionQuery extends Page<MemberTransaction> {

}
