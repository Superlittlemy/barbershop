-- V4: 营业时间(替换 Shop.businessHours 字符串字段) + 会员预约
-- 营业时间是预约时段的唯一依据,按"周内几号 + 起止 + 是否跨日"独立成表。

-- 1) 店铺营业时间
CREATE TABLE shop_business_hours (
    id              BIGINT NOT NULL COMMENT '营业时间ID' PRIMARY KEY,
    shop_id         BIGINT NOT NULL COMMENT '所属店铺ID',
    day_of_week     TINYINT NOT NULL COMMENT '周内几号:1=周一 ... 7=周日',
    start_time      TIME NOT NULL COMMENT '起始时间(HH:mm:ss)',
    end_time        TIME NOT NULL COMMENT '结束时间(HH:mm:ss)',
    cross_day       TINYINT NOT NULL DEFAULT 0 COMMENT '是否跨日:1=跨日(end_time<start_time)',
    sort_no         INT NOT NULL DEFAULT 0 COMMENT '排序号',
    created_by      BIGINT NULL COMMENT '创建人',
    created_time    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by      BIGINT NULL COMMENT '更新人',
    updated_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除:0.未删除 1.已删除',
    version         BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    INDEX idx_shop_dow (shop_id, day_of_week, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '店铺营业时间(周内几号 + 起止 + 是否跨日)';

-- 2) 删除 Shop.businessHours 字符串字段(由 shop_business_hours 表替代)
ALTER TABLE shop DROP COLUMN business_hours;

-- 3) 会员预约
CREATE TABLE appointment (
    id                BIGINT NOT NULL COMMENT '预约ID' PRIMARY KEY,
    shop_id           BIGINT NOT NULL COMMENT '所属店铺ID',
    member_id         BIGINT NOT NULL COMMENT '会员ID',
    service_item_id   BIGINT NOT NULL COMMENT '服务项目ID',
    appointment_date  DATE NOT NULL COMMENT '预约日期(yyyy-MM-dd)',
    start_time        TIME NOT NULL COMMENT '起始时段(HH:mm:ss)',
    end_time          TIME NOT NULL COMMENT '结束时段(HH:mm:ss,固定为 start_time + 1 小时)',
    status            TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0=有效(本期唯一值)',
    remark            VARCHAR(255) NULL COMMENT '备注',
    created_by        BIGINT NULL COMMENT '创建人',
    created_time      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by        BIGINT NULL COMMENT '更新人',
    updated_time      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted        TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除:0.未删除 1.已删除',
    version           BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    -- 同店铺同人同时段仅一条有效预约(is_deleted 加入联合键,允许软删后再次创建)
    UNIQUE KEY uk_shop_member_slot (shop_id, member_id, appointment_date, start_time, is_deleted),
    INDEX idx_shop_date (shop_id, appointment_date, is_deleted),
    INDEX idx_member (member_id, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '会员预约表';
