package com.slm.barbershop.converter;

import com.slm.barbershop.entity.Member;
import com.slm.barbershop.model.MemberRequest;
import com.slm.barbershop.model.MemberResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MemberConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdTime", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedTime", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "balance", ignore = true)
    Member toEntity(MemberRequest request);

    @Mapping(target = "shopId", source = "shopId")
    Member toEntityWithShopId(MemberRequest request, Long shopId);

    MemberResponse toResponse(Member member);

    List<MemberResponse> toResponseList(List<Member> members);

}