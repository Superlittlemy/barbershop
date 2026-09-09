package com.slm.barbershop.lock;

import com.slm.common.enums.ResultStatus;
import com.slm.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 乐观锁策略（默认）
 * <p>
 * 通过 MyBatis-Plus 的 @Version 注解与 {@link com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor}
 * 配合实现 CAS。重试 3 次,退避 50/100/200 ms + 抖动。
 */
@Slf4j
@Component
public class OptimisticLockStrategy implements LockStrategy {

    @Override
    public String getMode() {
        return DistributedLock.Mode.OPTIMISTIC.name();
    }

    @Override
    public boolean tryLock(String key, long timeout) {
        // 乐观锁不需要获取锁
        return true;
    }

    @Override
    public void unlock(String key) {
        // 乐观锁不需要显式释放
    }

    @Override
    public <T> T executeWithLock(String key, long timeout, LockAction<T> action) throws Throwable {
        try {
            return action.apply();
        } catch (OptimisticLockingFailureException e) {
            throw new DistributedLockException("数据已被修改");
        } catch (Exception e) {
            throw new BizException(ResultStatus.ERROR, "服务器内部错误");
        }
    }

}
