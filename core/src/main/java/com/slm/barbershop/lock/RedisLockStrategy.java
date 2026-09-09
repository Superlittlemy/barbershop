package com.slm.barbershop.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisLockStrategy implements LockStrategy {

    private final RedissonClient redissonClient;

    @Override
    public String getMode() {
        return DistributedLock.Mode.REDIS.name();
    }

    public RedisLockStrategy(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean tryLock(String key, long timeout) {
        RLock lock = redissonClient.getLock(key);
        try {
            // 尝试获取锁，等待时间0（不等待），超时时间timeout毫秒
            return lock.tryLock(0, timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(String key) {
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public <T> T executeWithLock(String key, long timeout, LockAction<T> action) throws Throwable {
        RLock lock = redissonClient.getLock(key);
        boolean locked = false;
        try {
            locked = lock.tryLock(0, timeout, TimeUnit.MILLISECONDS);
            if (!locked) {
                throw new DistributedLockException("获取Redis分布式锁失败: " + key);
            }
            return action.apply();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
