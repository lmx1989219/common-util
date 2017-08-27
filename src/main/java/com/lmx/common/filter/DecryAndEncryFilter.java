package com.lmx.common.filter;


import com.lmx.common.filter.spi.Decode;
import com.lmx.common.filter.spi.Encode;
import com.lmx.common.filter.wrapper.DecodeWrapServletRequest;
import com.lmx.common.filter.wrapper.EncodeWrapServletResponse;

import javax.servlet.*;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 请求响应报文加解密处理
 * Created by Administrator on 2017/8/26.
 */
public class DecryAndEncryFilter implements Filter {
    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
    Encode encode;
    Decode decode;

    public DecryAndEncryFilter(Encode encode, Decode decode) {
        this.encode = encode;
        this.decode = decode;
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        log.debug("filter config {}  ", filterConfig);
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        byte[] body = new byte[1024 * 10];
        int length = servletRequest.getInputStream().read(body);
        byte[] cp = new byte[length];
        System.arraycopy(body, 0, cp, 0, length);
        if (decode == null || encode == null)
            throw new RuntimeException("must implements encode and decode");
        cp = decode.decode(cp);
        log.debug("req={}", new String(cp));

        DecodeWrapServletRequest wrapServletRequest = new DecodeWrapServletRequest(servletRequest, cp);
        EncodeWrapServletResponse wrapServletResponse = new EncodeWrapServletResponse(servletResponse,
                new EncodeWrapServletResponse.ServletOutputStreamWrap());
        filterChain.doFilter(wrapServletRequest, wrapServletResponse);
        String resp = wrapServletResponse.getServletOutputStreamWrap().get();

        log.debug("resp={}", resp);
        PrintWriter printWriter = servletResponse.getWriter();
        printWriter.print(encode.encode(resp.getBytes()));
        printWriter.flush();
        printWriter.close();
    }

    public void destroy() {

    }
}
