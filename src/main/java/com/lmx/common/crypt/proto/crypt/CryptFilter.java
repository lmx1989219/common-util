package com.lmx.common.crypt.proto.crypt;


import com.lmx.common.crypt.proto.crypt.spi.Decrypt;
import com.lmx.common.crypt.proto.crypt.spi.Encrypt;
import com.lmx.common.crypt.proto.crypt.wrapper.DecryptWrapServletRequest;
import com.lmx.common.crypt.proto.crypt.wrapper.EncryptWrapServletResponse;

import javax.servlet.*;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 请求响应报文加解密处理
 * Created by Administrator on 2017/8/26.
 */
public class CryptFilter implements Filter {
    private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(getClass());
    private final static int BUF_SIZE = 1024 * 20;
    private Encrypt encode;
    private Decrypt decode;

    public CryptFilter(Encrypt encode, Decrypt decode) {
        this.encode = encode;
        this.decode = decode;
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        log.debug("spi config {}  ", filterConfig);
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (decode == null || encode == null) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else {
            byte[] body = new byte[BUF_SIZE];
            int length = servletRequest.getInputStream().read(body);
            byte[] cp = new byte[length];
            System.arraycopy(body, 0, cp, 0, length);
            cp = decode.decrypt(cp);
            log.debug("decrypt request body={}", new String(cp, "utf8"));

            DecryptWrapServletRequest wrapServletRequest = new DecryptWrapServletRequest(servletRequest, cp);
            EncryptWrapServletResponse wrapServletResponse = new EncryptWrapServletResponse(servletResponse,
                    new EncryptWrapServletResponse.ServletOutputStreamWrap());
            filterChain.doFilter(wrapServletRequest, wrapServletResponse);

            String resp = wrapServletResponse.getServletOutputStreamWrap().get();
            log.debug("origin response body={}", resp);

            PrintWriter printWriter = servletResponse.getWriter();
            printWriter.print(encode.encrypt(resp.getBytes()));
            printWriter.flush();
            printWriter.close();
        }
    }

    public void destroy() {
    }
}
