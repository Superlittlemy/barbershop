package com.slm.barbershop.converter;

import com.slm.barbershop.entity.ServiceCategory;
import com.slm.barbershop.model.ServiceCategoryRequest;
import com.slm.barbershop.model.ServiceCategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ServiceCategoryConverter {

    @Mapping(target = "sortNo", ignore = true)
    ServiceCategory toEntity(ServiceCategoryRequest request);

    @Mapping(target = "shopId", ignore = true)
    @Mapping(target = "sortNo", ignore = true)
    void updateEntity(@MappingTarget ServiceCategory category, ServiceCategoryRequest request);

    ServiceCategoryResponse toResponse(ServiceCategory category);

}
