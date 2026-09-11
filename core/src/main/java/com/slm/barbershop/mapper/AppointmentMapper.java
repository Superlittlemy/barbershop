package com.slm.barbershop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slm.barbershop.entity.Appointment;
import com.slm.barbershop.enums.AppointmentStatus;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Mapper
public interface AppointmentMapper extends BaseMapper<Appointment> {

    /**
     * 查询某店铺某日所有「有效」预约的起始时段(status=VALID)。
     * 用于可用时段计算时剔除已被占用的格子;已取消(CANCELLED)的预约不计入占用,时段可被重新预约。
     */
    default List<LocalTime> listStartTimes(Long shopId, LocalDate date) {
        return this.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Appointment>()
                        .select(Appointment::getStartTime)
                        .eq(Appointment::getShopId, shopId)
                        .eq(Appointment::getAppointmentDate, date)
                        .eq(Appointment::getStatus, AppointmentStatus.VALID.getCode()))
                .stream()
                .map(Appointment::getStartTime)
                .collect(java.util.stream.Collectors.toList());
    }

}
