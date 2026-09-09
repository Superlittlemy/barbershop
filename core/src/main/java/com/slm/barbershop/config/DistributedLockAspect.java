package com.slm.barbershop.config;

import com.slm.barbershop.lock.*;
import com.slm.barbershop.utils.RetryTemplate;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
public class DistributedLockAspect {

    @Autowired
    LockStrategyFactory lockStrategyFactory;

    @Autowired
    private RetryTemplate retryTemplate;

    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {

        // 解析锁Key（支持SpEL）
        String key = parseKey(distributedLock.key(), joinPoint);

        // 计算超时时间（毫秒）
        long timeoutMillis = distributedLock.timeUnit().toMillis(distributedLock.timeout());

        // 选择锁策略
        DistributedLock.Mode mode = distributedLock.mode();
        LockStrategy strategy = lockStrategyFactory.get(mode.name());

        // 执行重试逻辑
        boolean success = retryTemplate.executeWithRetry(
                distributedLock.retryTimes(),
                distributedLock.timeUnit().toMillis(distributedLock.waitTime()),
                distributedLock.backoff(),
                distributedLock.backoffBaseDelay(),
                distributedLock.backoffMaxDelay(),
                distributedLock.enableJitter(),
                () -> {
                    // 尝试获取锁
                    boolean locked = strategy.tryLock(key, timeoutMillis);
                    if (!locked) {
                        log.warn("获取锁失败，key={}", key);
                        return false;
                    }
                    return true;
                }
        );
        if (!success) {
            throw new DistributedLockException("已达到最大重试次数或超时");
        }

        log.info("获取分布式锁，key={}, mode={}, retryTimes={}", key, distributedLock.mode(), distributedLock.retryTimes());

        // 执行业务逻辑（带锁）
        try {
            return strategy.executeWithLock(key, timeoutMillis, joinPoint::proceed);
        } finally {
            // 释放锁
            strategy.unlock(key);
            log.info("释放分布式锁成功，key={}", key);
        }
    }

    /**
     * 解析SpEL表达式
     */
    private String parseKey(String keyExpression, ProceedingJoinPoint joinPoint) {
        try {
            // 如果包含#，说明是SpEL表达式
            if (keyExpression.contains("#")) {
                MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                Method method = signature.getMethod();
                Object[] args = joinPoint.getArgs();
                String[] paramNames = signature.getParameterNames();

                StandardEvaluationContext context = new StandardEvaluationContext();
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }

                // 也支持 #root 访问整个对象
                return parser.parseExpression(keyExpression).getValue(context, String.class);
            }
            return keyExpression;
        } catch (Exception e) {
            log.warn("解析SpEL表达式失败，使用原始key: {}", keyExpression, e);
            return keyExpression;
        }
    }

}
