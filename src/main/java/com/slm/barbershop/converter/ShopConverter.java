package com.slm.barbershop.converter;

import com.slm.barbershop.entity.Shop;
import com.slm.barbershop.model.ShopRequest;
import com.slm.barbershop.model.ShopResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ShopConverter {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdTime", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedTime", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    Shop toEntity(ShopRequest request);

    @Mapping(target = "userId", source = "userId")
    Shop toEntityWithUserId(ShopRequest request, Long userId);

    ShopResponse toResponse(Shop shop);

    List<ShopResponse> toResponseList(List<Shop> shops);

}