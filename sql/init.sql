CREATE DATABASE IF NOT EXISTS barbershop DEFAULT CHARACTER SET utf8mb4;

USE barbershop;

CREATE TABLE sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) COMMENT '密码（BCrypt加密）',
    email VARCHAR(255) COMMENT '邮箱',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE shop (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '店铺ID',
    name VARCHAR(100) NOT NULL COMMENT '店铺名称',
    logo VARCHAR(255) COMMENT '店铺Logo',
    address VARCHAR(255) COMMENT '地址',
    phone VARCHAR(20) COMMENT '联系电话',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除：0.未删除、1.已删除',
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺表';

CREATE TABLE member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会员ID',
    shop_id BIGINT NOT NULL COMMENT '所属店铺ID',
    name VARCHAR(50) NOT NULL COMMENT '会员姓名',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    avatar VARCHAR(255) DEFAULT '' COMMENT '会员头像URL',
    balance DECIMAL(10,2) DEFAULT 0 COMMENT '余额',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除：0.未删除、1.已删除',
    INDEX idx_shop_id (shop_id),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员表';

CREATE TABLE member_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '交易ID',
    member_id BIGINT NOT NULL COMMENT '会员ID',
    type VARCHAR(20) NOT NULL COMMENT '交易类型：STORE-储值 CONSUME-消费',
    amount DECIMAL(10,2) NOT NULL COMMENT '交易金额',
    balance_before DECIMAL(10,2) COMMENT '交易前余额',
    balance_after DECIMAL(10,2) COMMENT '交易后余额',
    remark VARCHAR(255) COMMENT '备注',
    idempotency_key VARCHAR(64) DEFAULT NULL COMMENT '幂等键(客户端UUID)',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_idempotency_key (idempotency_key),
    INDEX idx_member_id (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员交易流水表';

CREATE TABLE file_metadata (
    id BIGINT NOT NULL COMMENT '主键ID' PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    object_key VARCHAR(255) NOT NULL COMMENT 'MinIO对象Key',
    content_type VARCHAR(100) COMMENT 'MIME类型',
    size_bytes BIGINT NOT NULL COMMENT '文件大小(字节)',
    md5 VARCHAR(32) NOT NULL COMMENT 'MD5(小写hex)',
    access_url VARCHAR(512) NOT NULL COMMENT '公开访问URL',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '0.未删除 1.已删除',
    UNIQUE KEY uk_md5 (md5),
    INDEX idx_object_key (object_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件元数据表(内容寻址,按md5去重)';