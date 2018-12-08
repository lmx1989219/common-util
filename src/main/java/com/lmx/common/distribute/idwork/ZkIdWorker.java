package com.lmx.common.distribute.idwork;

import com.lmx.common.distribute.IdWorker;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.io.File;

/**
 * Created by Administrator on 2018/12/7.
 */
public class ZkIdWorker implements IdWorker {
    private RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    private CuratorFramework curatorFramework;
    private static final String ID_PATH = "/idWorker";

    public ZkIdWorker(String zookeeperConnectionString) throws Exception {
        curatorFramework = CuratorFrameworkFactory.newClient(zookeeperConnectionString, retryPolicy);
        curatorFramework.start();
        curatorFramework.create()
                .withMode(CreateMode.PERSISTENT).forPath(ID_PATH);
    }

    @Override
    public long nextId(String key) throws Exception {
        return Long.parseLong(curatorFramework.create()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(ID_PATH + "/" + key)
                .substring((ID_PATH + File.separator + key).length()));
    }

    @Override
    @Deprecated
    public long nextId() throws Exception {
        return Long.parseLong(curatorFramework.create()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(ID_PATH + "/DEFAULT")
                .substring(ID_PATH.length()));
    }
}
