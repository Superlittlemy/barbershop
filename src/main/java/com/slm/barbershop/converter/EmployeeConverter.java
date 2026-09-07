package com.slm.barbershop.converter;

import com.slm.barbershop.entity.Employee;
import com.slm.barbershop.model.EmployeeRequest;
import com.slm.barbershop.model.EmployeeResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmployeeConverter {

    Employee toEntity(EmployeeRequest request);

    /**
     * 更新时店铺与工号不可改(工号由系统生成,入参无此字段)
     */
    @Mapping(target = "shopId", ignore = true)
    void updateEntity(@MappingTarget Employee employee, EmployeeRequest request);

    EmployeeResponse toResponse(Employee employee);

    List<EmployeeResponse> toResponseList(List<Employee> employees);

}
