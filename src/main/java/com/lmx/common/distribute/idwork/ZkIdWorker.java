package com.lmx.common.distribute.idwork;

import com.lmx.common.distribute.IdWorker;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

/**
 * Created by Administrator on 2018/12/7.
 */
public class ZkIdWorker implements IdWorker {
    private RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    private CuratorFramework curatorFramework;

    public ZkIdWorker(String zookeeperConnectionString) {
        curatorFramework = CuratorFrameworkFactory.newClient(zookeeperConnectionString, retryPolicy);
        curatorFramework.start();
    }

    @Override
    public long nextId(String key) throws Exception {
        return Long.parseLong(curatorFramework.create()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(key));
    }

    @Override
    @Deprecated
    public long nextId() {
        return 0L;
    }
}
