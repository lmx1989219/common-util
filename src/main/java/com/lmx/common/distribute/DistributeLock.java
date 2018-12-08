package com.lmx.common.distribute;

import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2018/12/7.
 */
public interface DistributeLock {

    boolean tryLock(long time, TimeUnit timeUnit, String lockKey) throws Exception;

    void lock(String lockKey) throws Exception;

    void unLock() throws Exception;
}
