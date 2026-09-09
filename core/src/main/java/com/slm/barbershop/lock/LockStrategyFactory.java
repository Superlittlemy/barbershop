package com.slm.barbershop.lock;

import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 锁策略工厂
 * <p>
 * 根据 {@code member.lock.mode} 选择策略,默认 {@link OptimisticLockStrategy}。
 * 当前仅实现 OPTIMISTIC 模式;REDIS_LOCK 模式为预留,需引入 Redisson 后启用。
 */
@Getter
@Component
public class LockStrategyFactory implements ApplicationContextAware {

    private final Map<String, LockStrategy> strategies = new HashMap<>();

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        context.getBeansOfType(LockStrategy.class).values().forEach(strategy -> {
            strategies.put(strategy.getMode(), strategy);
        });
    }

    public LockStrategy get(String mode) {
        if (strategies.containsKey(mode)) {
            return strategies.get(mode);
        }
        throw new IllegalArgumentException("Invalid lock mode: " + mode);
    }

}
