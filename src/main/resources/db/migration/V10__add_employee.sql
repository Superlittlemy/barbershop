-- V10: 店铺员工 + 员工-服务项目关联 + 账单/预约关联员工
-- 员工工号由系统生成(店内递增,E0001 起);uk 含 is_deleted,软删后工号可释放复用
-- (对齐 appointment.uk_shop_member_slot 的设计)。
-- 账单仅在消费类型(CONSUME)关联员工;储值(STORE)不关联,employee_id 恒为 NULL。
-- 员工姓名不冗余存储,响应层实时 JOIN employee 表查询(对齐 V9 去冗余方向)。

USE barbershop;

-- 1) 店铺员工表
CREATE TABLE employee (
    id BIGINT NOT NULL COMMENT '主键ID' PRIMARY KEY,
    shop_id BIGINT NOT NULL COMMENT '所属店铺ID',
    employee_no VARCHAR(32) NOT NULL COMMENT '工号(系统生成,店内递增,如E0001)',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    gender TINYINT DEFAULT NULL COMMENT '性别:1.男 2.女(可空)',
    hire_date DATE DEFAULT NULL COMMENT '入职时间',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除:0.未删除 1.已删除',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    UNIQUE KEY uk_shop_employee_no (shop_id, employee_no, is_deleted),
    INDEX idx_shop_id (shop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺员工表';

-- 2) 员工-服务项目关联表(多选)
CREATE TABLE employee_service_item (
    id BIGINT NOT NULL COMMENT '主键ID' PRIMARY KEY,
    employee_id BIGINT NOT NULL COMMENT '员工ID',
    service_item_id BIGINT NOT NULL COMMENT '服务项目ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_employee_item (employee_id, service_item_id),
    INDEX idx_service_item_id (service_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工-服务项目关联表';

-- 3) 账单关联员工(消费账单必填,储值为空)
ALTER TABLE bill
    ADD COLUMN employee_id BIGINT DEFAULT NULL COMMENT '员工ID(消费账单关联;储值为空)' AFTER member_id,
    ADD INDEX idx_employee_id (employee_id);

-- 4) 预约关联员工(可空:未指定员工)
ALTER TABLE appointment
    ADD COLUMN employee_id BIGINT DEFAULT NULL COMMENT '员工ID(可空:未指定员工)' AFTER member_id,
    ADD INDEX idx_employee_id (employee_id);
