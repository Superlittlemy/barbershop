package com.slm.barbershop.converter;

import com.slm.barbershop.entity.Bill;
import com.slm.barbershop.model.BillResponse;
import com.slm.barbershop.model.TransactionItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BillConverter {

    /**
     * 显式声明字段映射,避免 MapStruct 因驼峰/同名问题漏掉 type 等新增字段
     */
    @Mappings({
            @Mapping(source = "id", target = "id"),
            @Mapping(source = "shopId", target = "shopId"),
            @Mapping(source = "memberId", target = "memberId"),
            @Mapping(source = "memberName", target = "memberName"),
            @Mapping(source = "employeeName", target = "employeeName"),
            @Mapping(source = "payChannel", target = "payChannel"),
            @Mapping(source = "type", target = "type"),
            @Mapping(source = "totalAmount", target = "totalAmount"),
            @Mapping(source = "remark", target = "remark"),
            @Mapping(source = "isCancelled", target = "isCancelled"),
            @Mapping(source = "cancelledTime", target = "cancelledTime"),
            @Mapping(source = "cancelledBy", target = "cancelledBy"),
            @Mapping(source = "cancelReason", target = "cancelReason"),
            @Mapping(source = "createdTime", target = "createdTime"),
    })
    BillResponse toResponse(Bill bill);

    default BillResponse toResponseWithItems(Bill bill, List<TransactionItemResponse> items) {
        BillResponse response = toResponse(bill);
        // 兜底: 显式把 type 写回,防止 converter 漏掉
        if (response.getType() == null && bill.getType() != null) {
            response.setType(bill.getType());
        }
        response.setItems(items == null ? Collections.emptyList() : items);
        return response;
    }

    List<BillResponse> toResponseList(List<Bill> bills);

}