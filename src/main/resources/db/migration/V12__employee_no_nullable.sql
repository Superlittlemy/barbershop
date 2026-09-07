-- V12: employee.employee_no 允许 NULL
-- 原因: uk_shop_employee_no (shop_id, employee_no, is_deleted) 包含 is_deleted,
--      "删除员工清除工号" 用于释放唯一键占位。employee_no NULL 不参与唯一键,
--      多个软删员工(NULL, is_deleted=1) 不再触发冲突。
-- 兼容性: 存量员工 is_deleted=0 的 NOT NULL 数据保持有效;软删后走应用层清号置 NULL。

USE barbershop;

ALTER TABLE employee
    MODIFY COLUMN employee_no VARCHAR(32) DEFAULT NULL COMMENT '工号(系统生成,店内递增,如E0001;软删后清空释放 uk 占位)';