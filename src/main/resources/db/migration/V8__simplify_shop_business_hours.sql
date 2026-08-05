-- V8: 简化营业时间设计
-- 删除 shop_business_hourses 表(原"按周内几号 + 起止 + 是否跨日"独立表)
-- 在 shop 表直接加 open_time / close_time / weekly_off
-- open_time <= close_time 强制(去除跨日)
-- weekly_off:7 位 0/1,索引 0=周一 ... 6=周日,'1'=休

DROP TABLE IF EXISTS shop_business_hours;

ALTER TABLE shop
    ADD COLUMN open_time  TIME       NOT NULL DEFAULT '09:00:00' AFTER phone,
    ADD COLUMN close_time TIME       NOT NULL DEFAULT '22:00:00' AFTER open_time,
    ADD COLUMN weekly_off VARCHAR(7) NOT NULL DEFAULT '0000000' AFTER close_time;
