-- 独立账单系统(与 member_transaction 物理隔离)
-- 支持:非会员/会员、单支付方式(MEMBER/OFFLINE/WECHAT/ALIPAY)、作废可逆、幂等键

USE barbershop;

CREATE TABLE bill (
    id BIGINT NOT NULL COMMENT '主键ID' PRIMARY KEY,
    shop_id BIGINT NOT NULL COMMENT '所属店铺ID',
    member_id BIGINT DEFAULT NULL COMMENT '会员ID(可空:非会员场景)',
    customer_name VARCHAR(50) DEFAULT NULL COMMENT '客户姓名(非会员快照)',
    customer_phone VARCHAR(20) DEFAULT NULL COMMENT '客户手机号(非会员快照)',
    pay_channel VARCHAR(20) NOT NULL COMMENT '支付方式:MEMBER-会员划账 OFFLINE-线下 WECHAT-微信 ALIPAY-支付宝',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '账单总金额',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    is_cancelled TINYINT NOT NULL DEFAULT 0 COMMENT '是否作废:0.正常 1.已作废',
    cancelled_time DATETIME DEFAULT NULL COMMENT '作废时间',
    cancelled_by BIGINT DEFAULT NULL COMMENT '作废操作人',
    cancel_reason VARCHAR(255) DEFAULT NULL COMMENT '作废原因',
    idempotency_key VARCHAR(64) DEFAULT NULL COMMENT '幂等键(客户端UUID)',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除:0.未删除 1.已删除',
    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    UNIQUE KEY uk_idempotency_key (idempotency_key),
    INDEX idx_shop_id (shop_id),
    INDEX idx_member_id (member_id),
    INDEX idx_pay_channel (pay_channel),
    INDEX idx_shop_time (shop_id, created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单主表';

CREATE TABLE bill_item (
    id BIGINT NOT NULL COMMENT '主键ID' PRIMARY KEY,
    bill_id BIGINT NOT NULL COMMENT '关联账单ID',
    item_id BIGINT NOT NULL COMMENT '服务项目ID',
    item_name VARCHAR(100) NOT NULL COMMENT '项目名(下单时冗余快照)',
    unit_price DECIMAL(10,2) NOT NULL COMMENT '下单时单价(冗余快照)',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    subtotal DECIMAL(10,2) NOT NULL COMMENT '小计',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_bill_id (bill_id),
    INDEX idx_item_id (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账单明细表';
