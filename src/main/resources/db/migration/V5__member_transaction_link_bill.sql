-- V5: 会员交易流水关联账单
-- 目的: 让会员消费同时进入 bill/bill_item(独立账单系统)
-- member_transaction_item 不再写入,明细统一存到 bill_item,通过 member_transaction.bill_id 反查

USE barbershop;

ALTER TABLE member_transaction
    ADD COLUMN bill_id BIGINT DEFAULT NULL COMMENT '关联账单ID(消费时指向 bill.id;储值为 NULL)' AFTER idempotency_key,
    ADD INDEX idx_member_tx_bill_id (bill_id);