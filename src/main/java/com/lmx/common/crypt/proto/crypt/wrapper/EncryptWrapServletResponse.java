
package com.lmx.common.crypt.proto.crypt.wrapper;


import com.google.api.client.util.Charsets;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

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
        private List<Byte> list = Lists.newArrayList();
        private Logger logger = LoggerFactory.getLogger(getClass());

        public ServletOutputStreamWrap() {
        }

        @Override
        public boolean isReady() {
            return list.size() > 0;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {

        }

        @Override
        public void write(int b) throws IOException {
            list.add((byte) b);
        }

        public String get() {
            byte[] resp = new byte[list.size()];
            for (int i = 0; i < list.size(); i++) {
                resp[i] = list.get(i);
            }
            try {
                return new String(resp, Charsets.UTF_8.toString());
            } catch (UnsupportedEncodingException e) {
                logger.error("", e);
                return "";
            }
        }
    }

}
