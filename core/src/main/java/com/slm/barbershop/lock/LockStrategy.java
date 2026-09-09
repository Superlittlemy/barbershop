package com.slm.barbershop.lock;

/**
 * 会员余额并发控制策略
 */
public interface LockStrategy {

    String getMode();

    /**
     * 尝试获取锁
     * @param key 锁Key
     * @param timeout 锁超时时间（毫秒）
     * @return true表示获取成功
     */
    boolean tryLock(String key, long timeout);

    /**
     * 释放锁
     * @param key 锁Key
     */
    void unlock(String key);

    /**
     * 执行带锁的业务逻辑
     * @param key 锁Key
     * @param timeout 超时时间
     * @param action 业务逻辑
     * @return 业务结果
     */
    <T> T executeWithLock(String key, long timeout, LockAction<T> action) throws Throwable;

    @FunctionalInterface
    interface LockAction<T> {
        T apply() throws Throwable;
    }

}
