-- V6: 账单加 type 列(消费 / 储值)
-- 会员储值同步进入账单系统: bill 表增加 type 列区分两种账单
--   CONSUME: 消费(MEMBER/OFFLINE/WECHAT/ALIPAY)
--   STORE:   储值(默认 OFFLINE)
-- 已有数据均为消费场景,统一回填 DEFAULT 'CONSUME'

USE barbershop;

ALTER TABLE bill
    ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'CONSUME' COMMENT '账单类型:CONSUME-消费 STORE-储值' AFTER pay_channel,
    ADD INDEX idx_type (type);