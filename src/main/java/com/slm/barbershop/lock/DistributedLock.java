package com.slm.barbershop.lock;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {

    /**
     * 锁的Key，支持SpEL表达式，如 "#userId"、"#user.id"
     */
    String key();

    /**
     * 锁模式
     */
    Mode mode() default DistributedLock.Mode.OPTIMISTIC;

    /**
     * 锁超时时间（自动释放）
     */
    long timeout() default 30;

    /**
     * 超时时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 等待锁的最长时间（获取锁失败后，在这个时间内重试）
     */
    long waitTime() default 5;

    /**
     * 重试次数（-1表示无限重试直到waitTime超时）
     */
    int retryTimes() default 3;

    /**
     * 重试退避策略
     */
    BackoffStrategy backoff() default BackoffStrategy.EXPONENTIAL;

    /**
     * 退避基础间隔（毫秒）
     */
    long backoffBaseDelay() default 100;

    /**
     * 最大退避间隔（毫秒），防止无限增长
     */
    long backoffMaxDelay() default 5000;

    /**
     * 是否启用随机抖动（防止惊群效应）
     */
    boolean enableJitter() default true;

    /**
     * 锁模式枚举
     */
    enum Mode {
        OPTIMISTIC,
        REDIS
    }

    /**
     * 退避策略枚举
     */
    enum BackoffStrategy {
        FIXED,        // 固定间隔
        LINEAR,       // 线性增长
        EXPONENTIAL   // 指数增长（默认）
    }

}
