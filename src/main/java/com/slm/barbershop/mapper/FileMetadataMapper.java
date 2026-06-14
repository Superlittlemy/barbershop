package com.slm.barbershop.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slm.barbershop.entity.FileMetadata;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileMetadataMapper extends BaseMapper<FileMetadata> {

    @Delete("DELETE FROM file_metadata WHERE md5 = #{md5} AND is_deleted = 1")
    int physicalDeleteSoftDeletedByMd5(@Param("md5") String md5);

}
