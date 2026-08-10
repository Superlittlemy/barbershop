-- V9: 账单表删除冗余的会员姓名/手机号字段
-- 原因: 同一会员在多次账单中的姓名/手机号冗余存储会导致历史账单与最新会员资料不一致,
--      且 Bill.customerName/Phone 已被废弃为会员快照(BillRequest 入参不再包含),
--      改为在响应层实时 JOIN member 表查询,确保展示最新会员资料。
-- 兼容性: 不再保留 customer_name / customer_phone 列;原快照数据随列删除,前端
--      通过 BillResponse.memberName / memberPhone(实时 JOIN)展示。

USE barbershop;

ALTER TABLE bill
    DROP COLUMN customer_name,
    DROP COLUMN customer_phone;