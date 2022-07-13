package com.lmx.common.oplog.filter;


import com.lmx.common.oplog.ContextHolder;
import com.lmx.common.oplog.util.OpLogUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 审计日志拦截器
 */
public class OpLogFilter implements Filter {
    private Logger log = LoggerFactory.getLogger(getClass());

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        ContextHolder.initContext();
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        request.getRequestURI();
        request.getSession();
        filterChain.doFilter(servletRequest, servletResponse);
        try {
            long start = System.currentTimeMillis();
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

    public void destroy() {
    }
}
