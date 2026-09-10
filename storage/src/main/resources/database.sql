CREATE TABLE file_metadata (
                               id BIGINT NOT NULL COMMENT '主键ID' PRIMARY KEY,
                               original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
                               object_key VARCHAR(255) NOT NULL COMMENT 'MinIO对象Key',
                               content_type VARCHAR(100) COMMENT 'MIME类型',
                               size_bytes BIGINT NOT NULL COMMENT '文件大小(字节)',
                               md5 VARCHAR(32) NOT NULL COMMENT 'MD5(小写hex)',
                               created_by BIGINT COMMENT '创建人',
                               created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                               updated_by BIGINT COMMENT '更新人',
                               updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                               is_deleted TINYINT DEFAULT 0 COMMENT '0.未删除 1.已删除',
                               UNIQUE KEY uk_md5 (md5),
                               INDEX idx_object_key (object_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件元数据表(内容寻址,按md5去重)';