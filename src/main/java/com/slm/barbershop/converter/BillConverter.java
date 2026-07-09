package com.slm.barbershop.converter;

import com.slm.barbershop.entity.Bill;
import com.slm.barbershop.model.BillResponse;
import com.slm.barbershop.model.TransactionItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BillConverter {

    BillResponse toResponse(Bill bill);

    default BillResponse toResponseWithItems(Bill bill, List<TransactionItemResponse> items) {
        BillResponse response = toResponse(bill);
        response.setItems(items == null ? Collections.emptyList() : items);
        return response;
    }

    List<BillResponse> toResponseList(List<Bill> bills);

}
