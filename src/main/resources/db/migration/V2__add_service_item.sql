-- V2: 消费项目(服务项)功能
-- 包含:
--   1) service_category   消费项目分类字典
--   2) service_item       消费项目字典
--   3) member_transaction_item  交易-项目明细
--   4) member_transaction 表补齐 BaseEntity 字段(便于历史数据接入规范)

CREATE TABLE service_category (
    id BIGINT NOT NULL COMMENT '分类ID' PRIMARY KEY,
    shop_id BIGINT NOT NULL COMMENT '所属店铺ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0.停用 1.启用',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除:0.未删除 1.已删除',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    INDEX idx_shop_id (shop_id),
    INDEX idx_shop_status (shop_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费项目分类字典表';

CREATE TABLE service_item (
    id BIGINT NOT NULL COMMENT '项目ID' PRIMARY KEY,
    shop_id BIGINT NOT NULL COMMENT '所属店铺ID',
    category_id BIGINT COMMENT '所属分类ID(可空:不分类)',
    name VARCHAR(100) NOT NULL COMMENT '项目名称',
    price DECIMAL(10,2) NOT NULL COMMENT '默认单价',
    description VARCHAR(500) COMMENT '项目描述',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0.下架 1.上架',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除:0.未删除 1.已删除',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    INDEX idx_shop_id (shop_id),
    INDEX idx_category_id (category_id),
    INDEX idx_shop_status (shop_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费项目字典表';

CREATE TABLE member_transaction_item (
    id BIGINT NOT NULL COMMENT '明细ID' PRIMARY KEY,
    transaction_id BIGINT NOT NULL COMMENT '关联交易ID',
    item_id BIGINT NOT NULL COMMENT '关联项目ID',
    item_name VARCHAR(100) NOT NULL COMMENT '项目名(冗余快照)',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '下单时单价(冗余快照)',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    subtotal DECIMAL(10,2) NOT NULL COMMENT '小计 = unit_price * quantity',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_item_id (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员交易-消费项目明细表';

-- 补齐 member_transaction 的 BaseEntity 字段
ALTER TABLE member_transaction
    ADD COLUMN created_by  BIGINT NULL COMMENT '创建人' AFTER idempotency_key,
    ADD COLUMN updated_by  BIGINT NULL COMMENT '更新人' AFTER created_by,
    ADD COLUMN updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER updated_by,
    ADD COLUMN is_deleted  TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除:0.未删除 1.已删除' AFTER updated_time,
    ADD COLUMN version     BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER is_deleted;
