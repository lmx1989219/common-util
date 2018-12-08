package com.lmx.common.clog;

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
     * appender 格式只保留msg
     */
    private Logger log_es = LoggerFactory.getLogger("es_log");

    /**
     * 仅在声明注解的时候启用日志收集
     *
     * @param opLogCollect
     */
    @Before("@annotation(opLogCollect)")
    public void adviceBefore(final OpLog opLogCollect) {
        ContextHolder.initContext();
    }

    /**
     * 在controller层声明注解
     *
     * @param opLogCollect
     */
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
                        printLog(l_.get(i), opLogCollect);
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

    private void printLog(List l, OpLog opLogCollect) {
        OperationLog operationLog = new OperationLog();
        int i = 0;
        //偶数为改前值，奇数为改后值
        for (i = 2 * i; 2 * i < l.size(); ++i) {
            String oldVal = (String) l.get(2 * i);
            String newVal = (String) l.get(2 * i + 1);
            operationLog.setModifyValue(newVal);
            operationLog.setOriginalValue(oldVal);
            //json结构输出，让filebeats完成后续导入kafka
            log_es.info(operationLog.toString());
        }
    }

}
