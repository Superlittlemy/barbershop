package com.slm.barbershop.converter;

import com.slm.barbershop.entity.MemberTransaction;
import com.slm.barbershop.model.MemberTransactionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MemberTransactionConverter {

    MemberTransactionResponse toResponse(MemberTransaction transaction);

    List<MemberTransactionResponse> toResponseList(List<MemberTransaction> transactions);

}