package com.lmx.common.oplog;

import com.lmx.common.oplog.util.OpLogUtil;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Aspect
public class OpLogAspect {
    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 仅在声明注解的时候启用日志收集
     *
     * @param opLogCollect
     */
    @Before("@annotation(opLogCollect)")
    public void adviceBefore(final OpLog opLogCollect) {
        ContextHolder.initContext();
    }

    @AfterReturning("@annotation(opLogCollect)")
    public void adviceAfterReturning(final OpLog opLogCollect) {
        try {
            long start = System.currentTimeMillis();
            RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            request.getRequestURI();
            request.getSession();
            //抓取上下文数据
            Object obj = ContextHolder.getContext();
            if (obj instanceof List) {
                List<List> l_ = (List) obj;
                for (int i = 0; i < l_.size(); ++i) {
                    //清理threadlocal
                    if (i % 2 == 0) {
                        ((ThreadLocal) l_.get(i)).remove();
                    } else {
                        OpLogUtil.printLog(l_.get(i), opLogCollect);
                    }
                }
            }
            log.info("operator log end cost: {}ms", System.currentTimeMillis() - start);
        } catch (Throwable throwable) {
            log.error("", throwable);
        } finally {
            ContextHolder.clear();
        }
    }

}
