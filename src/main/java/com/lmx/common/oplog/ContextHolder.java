package com.lmx.common.oplog;

import com.google.common.collect.Lists;

import java.util.List;

public class ContextHolder {
    public static final ThreadLocal threadLocal = new ThreadLocal();

    /**
     * 上下文内容
     *
     * @param val=指标列表，格式[oldVal,newVal,....]
     */
    public static void set(Object val) {
        if (!hasContext()) {
            threadLocal.set(Lists.newArrayList());
        }
        ((List) threadLocal.get()).add(val);
    }

    public static Object getContext() {
        return threadLocal.get();
    }


    public static void initContext() {
        if (threadLocal.get() == null) {
            threadLocal.set(Lists.newArrayList());
        }
    }

    public static boolean hasContext() {
        return threadLocal.get() != null;
    }

    public static void clear() {
        threadLocal.remove();
    }
}
