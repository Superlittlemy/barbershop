package com.slm.barbershop.converter;

import com.slm.barbershop.entity.Shop;
import com.slm.barbershop.model.ShopRequest;
import com.slm.barbershop.model.ShopResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ShopConverter {

    void updateEntity(@MappingTarget Shop shop, ShopRequest request);

    @Mapping(target = "userId", source = "userId")
    Shop toEntityWithUserId(ShopRequest request, Long userId);

    ShopResponse toResponse(Shop shop);

    List<ShopResponse> toResponseList(List<Shop> shops);

}