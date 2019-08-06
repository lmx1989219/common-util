
package com.lmx.common.crypt.proto.crypt.wrapper;


import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

/**
 * Created by Administrator on 2017/8/27.
 */
public class EncryptWrapServletResponse extends HttpServletResponseWrapper {
    private ServletOutputStreamWrap servletOutputStreamWrap;

    public EncryptWrapServletResponse(ServletResponse response, ServletOutputStreamWrap servletOutputStreamWrap) {
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
        private StringBuilder stringBuilder = new StringBuilder();

        public ServletOutputStreamWrap() {
        }

        @Override
        public boolean isReady() {
            return stringBuilder.length() > 0;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {

        }

        @Override
        public void write(int b) throws IOException {
            stringBuilder.append((char) b);
        }

        public String get() {
            try {
                return stringBuilder.toString();
            } finally {
                stringBuilder = null;
            }
        }
    }

}
