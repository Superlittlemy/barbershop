package com.slm.barbershop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slm.barbershop.entity.Member;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {

}