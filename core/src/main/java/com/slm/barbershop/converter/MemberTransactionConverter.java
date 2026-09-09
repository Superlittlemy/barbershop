package com.slm.barbershop.converter;

import com.slm.barbershop.entity.MemberTransaction;
import com.slm.barbershop.model.MemberTransactionResponse;
import com.slm.barbershop.model.TransactionItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MemberTransactionConverter {

    MemberTransactionResponse toResponse(MemberTransaction transaction);

    default MemberTransactionResponse toResponseWithItems(MemberTransaction transaction,
                                                          List<TransactionItemResponse> items) {
        MemberTransactionResponse response = toResponse(transaction);
        response.setItems(items == null ? Collections.emptyList() : items);
        return response;
    }

    List<MemberTransactionResponse> toResponseList(List<MemberTransaction> transactions);

}
