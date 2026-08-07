package com.slm.barbershop.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.slm.barbershop.entity.Appointment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 预约分页查询条件
 * <p>
 * 继承 {@link com.baomidou.mybatisplus.extension.plugins.pagination.Page}，实现"分页 + 查询"合一。
 * <p>
 * date 非空时按日期过滤;keyword 非空时按会员姓名/服务项目名模糊匹配(OR 关系,LIKE)。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "预约分页查询")
public class AppointmentPageQuery extends Page<Appointment> {

    @NotNull(message = "店铺ID不能为空")
    @Schema(description = "店铺ID")
    private Long shopId;

    @Schema(description = "预约日期(yyyy-MM-dd,可选)")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @Schema(description = "关键词:会员姓名/服务项目名模糊匹配(OR 关系,LIKE,可选)")
    private String keyword;

}