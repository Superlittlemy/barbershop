package com.slm.barbershop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slm.barbershop.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

}