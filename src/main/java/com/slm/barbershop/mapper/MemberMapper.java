package com.slm.barbershop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slm.barbershop.entity.Member;
import com.slm.barbershop.model.MemberMatchVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberMapper extends BaseMapper<Member> {

    List<MemberMatchVO> matchAcrossShops(@Param("phone") String phone);

}