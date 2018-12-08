package com.lmx.common.distribute.lock;

import com.lmx.common.distribute.DistributeLock;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.util.concurrent.TimeUnit;

/**
 * Created by Administrator on 2018/12/7.
 */
public class ZkLock implements DistributeLock {
    private RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    private CuratorFramework curatorFramework;
    private ThreadLocal<InterProcessMutex> threadLocal = new ThreadLocal<>();
    private static final String LOCK_PATH = "/lock";

    public ZkLock(String zookeeperConnectionString) throws Exception {
        curatorFramework = CuratorFrameworkFactory.newClient(zookeeperConnectionString, retryPolicy);
        curatorFramework.start();
    }

    @Override
    public boolean tryLock(long time, TimeUnit timeUnit, String lockKey) throws Exception {
        InterProcessMutex lock = new InterProcessMutex(curatorFramework, LOCK_PATH + "/" + lockKey);
        try {
            return lock.acquire(time, timeUnit);
        } finally {
            threadLocal.set(lock);
        }
    }

    @Override
    public void lock(String lockKey) throws Exception {
        InterProcessMutex lock = new InterProcessMutex(curatorFramework, LOCK_PATH + "/" + lockKey);
        lock.acquire();
        threadLocal.set(lock);
    }

    @Override
    public void unLock() throws Exception {
        threadLocal.get().release();
        threadLocal.remove();
    }
}
