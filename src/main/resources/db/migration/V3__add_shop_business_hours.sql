-- V3: 店铺新增营业时间字段
ALTER TABLE shop
    ADD COLUMN business_hours VARCHAR(100) NULL COMMENT '营业时间(如: 09:00-22:00)' AFTER phone;
