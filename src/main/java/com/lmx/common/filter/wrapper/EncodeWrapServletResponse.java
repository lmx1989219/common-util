
package com.lmx.common.filter.wrapper;


import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * Created by Administrator on 2017/8/27.
 */
public class EncodeWrapServletResponse extends HttpServletResponseWrapper {
    ServletOutputStreamWrap servletOutputStreamWrap;

    public EncodeWrapServletResponse(ServletResponse response, ServletOutputStreamWrap servletOutputStreamWrap) {
        super((HttpServletResponse) response);
        this.servletOutputStreamWrap = servletOutputStreamWrap;
    }

    public ServletOutputStreamWrap getServletOutputStreamWrap() {
        return servletOutputStreamWrap;
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return servletOutputStreamWrap;
    }

    public static class ServletOutputStreamWrap extends ServletOutputStream {
        StringBuilder stringBuilder = new StringBuilder();

        public ServletOutputStreamWrap() {
        }

        @Override
        public void write(int b) throws IOException {
            stringBuilder.append((char) b);
        }

        public String get() {
            return stringBuilder.toString();
        }
    }

}
