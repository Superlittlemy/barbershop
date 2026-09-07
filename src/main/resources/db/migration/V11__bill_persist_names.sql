-- V11: 账单持久化会员/员工姓名快照
-- 原因: V9 改为响应层实时 JOIN 后,会员或员工被删除(软删不可见)时历史账单的客户/服务人员展示会消失;
--      对齐 bill_item.item_name 的"下单时快照"模式,开单时把姓名写入账单,之后改档不回写历史账单。
-- 兼容性: 列允许 NULL;存量账单用当前最新资料一次性回填。响应字段结构不变(BillResponse 同名字段改为读列)。

USE barbershop;

ALTER TABLE bill
    ADD COLUMN member_name VARCHAR(50) DEFAULT NULL COMMENT '会员姓名(开单时快照)' AFTER member_id,
    ADD COLUMN employee_name VARCHAR(50) DEFAULT NULL COMMENT '员工姓名(开单时快照)' AFTER employee_id;

-- 存量数据回填当前最新资料(软删的会员/员工同样纳入回填)
UPDATE bill b
    INNER JOIN member m ON m.id = b.member_id
SET b.member_name = m.name
WHERE b.member_id IS NOT NULL AND b.member_name IS NULL;

UPDATE bill b
    INNER JOIN employee e ON e.id = b.employee_id
SET b.employee_name = e.name
WHERE b.employee_id IS NOT NULL AND b.employee_name IS NULL;
