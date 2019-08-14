package com.lmx.common.distribute.idwork;

import com.lmx.common.distribute.IdWorker;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * id顺序递增，不重复
 * Created by Administrator on 2018/12/7.
 */
public class ZkIdWorker implements IdWorker {
    /**
     * 重试策略：延迟一秒执行，试三次
     */
    private RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
    private CuratorFramework curatorFramework;
    private static final String ID_PATH = "/idWorker";
    /**
     * 定时清理已使用的节点数据
     */
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private Map<String, String> ids = new ConcurrentHashMap<>(2 << 16);

    public ZkIdWorker(String zookeeperConnectionString) throws Exception {
        curatorFramework = CuratorFrameworkFactory.newClient(zookeeperConnectionString, retryPolicy);
        curatorFramework.start();
        if (curatorFramework.checkExists().forPath(ID_PATH) == null)
            curatorFramework.create()
                    .withMode(CreateMode.PERSISTENT).forPath(ID_PATH);
        scheduledExecutorService.schedule(() -> {
            for (String idPath : ids.keySet()) {
                try {
                    curatorFramework.delete().forPath(idPath);
                } catch (Exception e) {
                }
            }
        }, 5, TimeUnit.MINUTES);
    }

    @Override
    public long nextId(String key) throws Exception {
        DistributedAtomicLong distributedAtomicLong =
                new DistributedAtomicLong(curatorFramework, ID_PATH + "/" + key, retryPolicy);
        AtomicValue<Long> atomicValue = distributedAtomicLong.increment();
        if (atomicValue.succeeded())
            return atomicValue.postValue();
        else
            throw new RuntimeException("");
    }

    @Override
    public long nextId() throws Exception {
        String idStr = curatorFramework.create()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath(ID_PATH + "/DEFAULT");
        String idSeq = idStr.substring((ID_PATH + "/DEFAULT").length());
        ids.put(idStr, "");
        return Long.parseLong(idSeq);
    }

}
