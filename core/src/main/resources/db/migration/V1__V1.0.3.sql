CREATE DATABASE IF NOT EXISTS barbershop DEFAULT CHARACTER SET utf8mb4;

USE barbershop;

CREATE TABLE sys_user (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
                          username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
                          password VARCHAR(255) COMMENT '密码（BCrypt加密）',
                          email VARCHAR(255) COMMENT '邮箱',
                          created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          UNIQUE INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE shop (
                      id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '店铺ID',
                      name VARCHAR(100) NOT NULL COMMENT '店铺名称',
                      logo VARCHAR(255) COMMENT '店铺Logo(objectKey)',
                      address VARCHAR(255) COMMENT '地址',
                      phone VARCHAR(20) COMMENT '联系电话',
                      open_time  TIME       NOT NULL DEFAULT '09:00:00',
                      close_time TIME       NOT NULL DEFAULT '22:00:00',
                      weekly_off VARCHAR(7) NOT NULL DEFAULT '0000000',
                      business_hours VARCHAR(100) NULL COMMENT '营业时间(如: 09:00-22:00)',
                      user_id BIGINT NOT NULL COMMENT '所属用户ID',
                      created_by BIGINT COMMENT '创建人',
                      created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                      updated_by BIGINT COMMENT '更新人',
                      updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                      is_deleted TINYINT DEFAULT 0 COMMENT '是否删除：0.未删除、1.已删除',
                      INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺表';

CREATE TABLE member (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会员ID',
                        shop_id BIGINT NOT NULL COMMENT '所属店铺ID',
                        name VARCHAR(50) NOT NULL COMMENT '会员姓名',
                        phone VARCHAR(20) NOT NULL COMMENT '手机号',
                        avatar VARCHAR(255) DEFAULT '' COMMENT '会员头像(objectKey)',
                        balance DECIMAL(10,2) DEFAULT 0 COMMENT '余额',
                        version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
                        created_by BIGINT COMMENT '创建人',
                        created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        updated_by BIGINT COMMENT '更新人',
                        updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        is_deleted TINYINT DEFAULT 0 COMMENT '是否删除：0.未删除、1.已删除',
                        INDEX idx_shop_id (shop_id),
                        INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员表';

CREATE TABLE member_transaction (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '交易ID',
                                    member_id BIGINT NOT NULL COMMENT '会员ID',
                                    type VARCHAR(20) NOT NULL COMMENT '交易类型：STORE-储值 CONSUME-消费',
                                    amount DECIMAL(10,2) NOT NULL COMMENT '交易金额',
                                    balance_before DECIMAL(10,2) COMMENT '交易前余额',
                                    balance_after DECIMAL(10,2) COMMENT '交易后余额',
                                    remark VARCHAR(255) COMMENT '备注',
                                    idempotency_key VARCHAR(64) DEFAULT NULL COMMENT '幂等键(客户端UUID)',
                                    bill_id BIGINT DEFAULT NULL COMMENT '关联账单ID(消费时指向 bill.id;储值为 NULL)',
                                    version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
                                    created_by BIGINT COMMENT '创建人',
                                    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                    updated_by BIGINT COMMENT '更新人',
                                    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                    is_deleted TINYINT DEFAULT 0 COMMENT '是否删除：0.未删除、1.已删除',
                                    UNIQUE KEY uk_idempotency_key (idempotency_key),
                                    INDEX idx_member_id (member_id),
                                    INDEX idx_member_tx_bill_id (bill_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员交易流水表';

CREATE TABLE service_category (
                                  id BIGINT NOT NULL COMMENT '分类ID' PRIMARY KEY,
                                  shop_id BIGINT NOT NULL COMMENT '所属店铺ID',
                                  name VARCHAR(50) NOT NULL COMMENT '分类名称',
                                  sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
                                  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0.停用 1.启用',
                                  created_by BIGINT COMMENT '创建人',
                                  created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  updated_by BIGINT COMMENT '更新人',
                                  updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除:0.未删除 1.已删除',
                                  version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
                                  INDEX idx_shop_id (shop_id),
                                  INDEX idx_shop_status (shop_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费项目分类字典表';

CREATE TABLE service_item (
                              id BIGINT NOT NULL COMMENT '项目ID' PRIMARY KEY,
                              shop_id BIGINT NOT NULL COMMENT '所属店铺ID',
                              category_id BIGINT COMMENT '所属分类ID(可空:不分类)',
                              name VARCHAR(100) NOT NULL COMMENT '项目名称',
                              price DECIMAL(10,2) NOT NULL COMMENT '默认单价',
                              description VARCHAR(500) COMMENT '项目描述',
                              status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0.下架 1.上架',
                              created_by BIGINT COMMENT '创建人',
                              created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              updated_by BIGINT COMMENT '更新人',
                              updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                              is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除:0.未删除 1.已删除',
                              version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
                              INDEX idx_shop_id (shop_id),
                              INDEX idx_category_id (category_id),
                              INDEX idx_shop_status (shop_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消费项目字典表';

CREATE TABLE member_transaction_item (
                                         id BIGINT NOT NULL COMMENT '明细ID' PRIMARY KEY,
                                         transaction_id BIGINT NOT NULL COMMENT '关联交易ID',
                                         item_id BIGINT NOT NULL COMMENT '关联项目ID',
                                         item_name VARCHAR(100) NOT NULL COMMENT '项目名(冗余快照)',
                                         unit_price DECIMAL(10,2) NOT NULL COMMENT '下单时单价(冗余快照)',
                                         quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
                                         subtotal DECIMAL(10,2) NOT NULL COMMENT '小计 = unit_price * quantity',
                                         sort_no INT NOT NULL DEFAULT 0 COMMENT '排序号',
                                         created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         INDEX idx_transaction_id (transaction_id),
                                         INDEX idx_item_id (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员交易-消费项目明细表';

CREATE TABLE bill (
                      id BIGINT NOT NULL COMMENT '主键ID' PRIMARY KEY,
                      shop_id BIGINT NOT NULL COMMENT '所属店铺ID',
                      member_id BIGINT DEFAULT NULL COMMENT '会员ID(可空:非会员场景)',
                      member_name VARCHAR(50) DEFAULT NULL COMMENT '会员姓名(开单时快照)',
                      employee_id BIGINT DEFAULT NULL COMMENT '员工ID(消费账单关联;储值为空)',
                      employee_name VARCHAR(50) DEFAULT NULL COMMENT '员工姓名(开单时快照)',
                      pay_channel VARCHAR(20) NOT NULL COMMENT '支付方式:MEMBER-会员划账 OFFLINE-线下 WECHAT-微信 ALIPAY-支付宝',
                      type VARCHAR(20) NOT NULL DEFAULT 'CONSUME' COMMENT '账单类型:CONSUME-消费 STORE-储值',
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
                      INDEX idx_type (type),
                      INDEX idx_employee_id (employee_id)
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

CREATE TABLE appointment (
                             id                BIGINT NOT NULL COMMENT '预约ID' PRIMARY KEY,
                             shop_id           BIGINT NOT NULL COMMENT '所属店铺ID',
                             member_id         BIGINT NOT NULL COMMENT '会员ID',
                             employee_id BIGINT DEFAULT NULL COMMENT '员工ID(可空:未指定员工)',
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
                             INDEX idx_shop_date (shop_id, appointment_date, is_deleted),
                             INDEX idx_member (member_id, is_deleted),
                             INDEX idx_employee_id (employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '会员预约表';

CREATE TABLE employee (
                          id BIGINT NOT NULL COMMENT '主键ID' PRIMARY KEY,
                          shop_id BIGINT NOT NULL COMMENT '所属店铺ID',
                          employee_no VARCHAR(32) NULL COMMENT '工号(系统生成,店内递增,如E0001)',
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