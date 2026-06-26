package com.slm.barbershop.converter;

import com.slm.barbershop.entity.ServiceItem;
import com.slm.barbershop.model.ServiceItemRequest;
import com.slm.barbershop.model.ServiceItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ServiceItemConverter {

    void updateEntity(@MappingTarget ServiceItem item, ServiceItemRequest request);

    ServiceItemResponse toResponse(ServiceItem item);

    List<ServiceItemResponse> toResponseList(List<ServiceItem> items);

}
