package com.slm.storage.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.slm.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件元数据
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_metadata")
public class FileMetadata extends BaseEntity {

    /**
     * 原始文件名
     */
    @TableField("original_name")
    private String originalName;

    /**
     * MinIO对象Key
     */
    @TableField("object_key")
    private String objectKey;

    /**
     * MIME类型
     */
    @TableField("content_type")
    private String contentType;

    /**
     * 文件大小(字节)
     */
    @TableField("size_bytes")
    private Long sizeBytes;

    /**
     * MD5(小写hex)
     */
    @TableField("md5")
    private String md5;

}
