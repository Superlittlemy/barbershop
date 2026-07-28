package com.slm.barbershop.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 账单分页响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "账单分页响应")
public class BillPageVO {

    @Schema(description = "当前页数据")
    private List<BillResponse> records;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "当前页(从1开始)")
    private Long page;

    @Schema(description = "每页大小")
    private Long size;

    @Schema(description = "是否还有更多")
    private Boolean hasMore;

}
