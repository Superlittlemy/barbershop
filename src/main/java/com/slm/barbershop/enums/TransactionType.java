package com.slm.barbershop.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 交易类型枚举
 */
@Getter
@AllArgsConstructor
public enum TransactionType {

    STORE("储值"),
    CONSUME("消费");

    private final String description;

}