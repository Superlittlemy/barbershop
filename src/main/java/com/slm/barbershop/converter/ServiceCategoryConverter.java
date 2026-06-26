package com.slm.barbershop.converter;

import com.slm.barbershop.entity.ServiceCategory;
import com.slm.barbershop.model.ServiceCategoryRequest;
import com.slm.barbershop.model.ServiceCategoryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ServiceCategoryConverter {

    void updateEntity(@MappingTarget ServiceCategory category, ServiceCategoryRequest request);

    ServiceCategoryResponse toResponse(ServiceCategory category);

    List<ServiceCategoryResponse> toResponseList(List<ServiceCategory> categories);

}
