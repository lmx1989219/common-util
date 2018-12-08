package com.lmx.common.distribute;

/**
 * Created by Administrator on 2018/12/7.
 */
public interface IdWorker {
    long nextId(String key) throws Exception;

    long nextId() throws Exception;
}
