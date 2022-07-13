package com.lmx.common.oplog;

import com.lmx.common.oplog.util.OpLogUtil;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Aspect
//最高优先级，比如出现在事务注解上层
@Order(-1)
public class OpLogAspect {
    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 在声明注解的时候启用日志收集
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
            Object logObj = ContextHolder.getContext();
            if (logObj instanceof List) {
                List<List> logList = (List) logObj;
                if (!CollectionUtils.isEmpty(logList))
                    logList.forEach(OpLogUtil::printLog);

            }
            log.info("operator log end cost: {}ms", System.currentTimeMillis() - start);
        } catch (Throwable throwable) {
            log.error("", throwable);
        } finally {
            ContextHolder.clear();
        }
    }

}
