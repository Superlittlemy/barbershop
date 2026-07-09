package com.slm.barbershop.converter;

import com.slm.barbershop.entity.Appointment;
import com.slm.barbershop.model.AppointmentResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppointmentConverter {

    /**
     * entity → response。友好字段(memberName/serviceItemName/shopName)在 service 层按 id 批量回填,
     * 避免 controller 关心跨实体装配并防止 N+1。
     */
    AppointmentResponse toResponse(Appointment appointment);

    List<AppointmentResponse> toResponseList(List<Appointment> appointments);

}
