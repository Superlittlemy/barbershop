package com.slm.barbershop.utils;

import com.slm.barbershop.lock.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Slf4j
@Component
public class RetryTemplate {

    /**
     * 执行带重试的操作
     *
     * @param retryTimes      重试次数
     * @param waitTime        总等待时间（毫秒），超时则放弃
     * @param backoffStrategy 退避策略
     * @param baseDelay       基础延迟（毫秒）
     * @param maxDelay        最大延迟（毫秒）
     * @param enableJitter    是否启用抖动
     * @param action          业务操作，返回true表示成功
     * @return true表示最终成功
     */
    public boolean executeWithRetry(
            int retryTimes,
            long waitTime,
            DistributedLock.BackoffStrategy backoffStrategy,
            long baseDelay,
            long maxDelay,
            boolean enableJitter,
            Supplier<Boolean> action) {

        long startTime = System.currentTimeMillis();
        int attempt = 0;
        int maxAttempts = retryTimes == -1 ? Integer.MAX_VALUE : retryTimes;

        while (true) {
            attempt++;
            try {
                // 执行业务操作
                if (action.get()) {
                    return true;
                }
            } catch (Exception e) {
                log.warn("第{}次执行失败", attempt, e);
            }

            // 检查是否达到重试上限
            if (attempt >= maxAttempts) {
                log.warn("达到最大重试次数 {}，放弃执行", maxAttempts);
                return false;
            }

            // 检查是否超时
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= waitTime) {
                log.warn("等待超时 {}ms，放弃执行", waitTime);
                return false;
            }

            // 计算下次重试延迟
            long delay = calculateDelay(attempt, backoffStrategy, baseDelay, maxDelay, enableJitter);

            // 确保不超过剩余等待时间
            long remaining = waitTime - elapsed;
            delay = Math.min(delay, remaining);

            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    /**
     * 计算退避延迟（核心算法）
     */
    private long calculateDelay(
            int attempt,
            DistributedLock.BackoffStrategy strategy,
            long baseDelay,
            long maxDelay,
            boolean enableJitter) {

        long delay;

        switch (strategy) {
            case FIXED:
                delay = baseDelay;
                break;

            case LINEAR:
                // 线性增长：baseDelay * attempt
                delay = baseDelay * attempt;
                break;

            case EXPONENTIAL:
            default:
                // 指数增长：baseDelay * 2^(attempt-1)
                delay = baseDelay * (1L << Math.min(attempt - 1, 20)); // 防止溢出
                break;
        }

        // 限制最大值
        delay = Math.min(delay, maxDelay);

        // 添加随机抖动（±25%），防止惊群效应
        if (enableJitter) {
            double jitterFactor = 0.75 + ThreadLocalRandom.current().nextDouble() * 0.5; // 0.75 ~ 1.25
            delay = (long) (delay * jitterFactor);
        }

        return delay;
    }

}
