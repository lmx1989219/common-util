package com.lmx.common.oplog;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 拦截更新前、后操作日志
 */
public class OpHibernateInterceptor extends EmptyInterceptor {
    private ThreadLocal updates = new ThreadLocal();
    private ThreadLocal caches = new ThreadLocal();

    public boolean onFlushDirty(Object entity, Serializable id,
                                Object[] currentState, Object[] previousState,
                                String[] propertyNames, Type[] types)
            throws CallbackException {
        if (ContextHolder.hasContext()) {
            if (caches.get() == null) {
                caches.set(Maps.newHashMap());
            }
            for (int i = 0; i < previousState.length; ++i) {
                String oldVal = String.valueOf(previousState[i]);
                String newVal = String.valueOf(currentState[i]);
                String exsits = oldVal + "" + newVal;
                //有差异说明发生修改，缓存起来
                if (previousState[i] != null &&
                        !oldVal.equals(newVal)
                        && !((Map) caches.get()).containsKey(exsits)) {
                    ((Map) caches.get()).put(exsits, "");
                    cache(updates, oldVal);
                    cache(updates, newVal);
                }
            }
        }
        return false;
    }

    private void cache(ThreadLocal<Object> threadLocal, Object entity) {
        if (threadLocal.get() == null) {
            List<Object> list = Lists.newArrayList();
            list.add(entity);
            threadLocal.set(list);
        } else {
            ((List) threadLocal.get()).add(entity);
        }
    }

    public void postFlush(Iterator iterator) {
        if (ContextHolder.hasContext()) {
            try {
                Object uObj;
                if ((uObj = updates.get()) != null) {
                    ContextHolder.set(caches);
                    ContextHolder.set(uObj);
                }
            } finally {
                updates.remove();
            }
        }
    }

    @Override
    public void afterTransactionCompletion(Transaction tx) {
        if (ContextHolder.hasContext()) {
            updates.remove();
        }
    }

}
