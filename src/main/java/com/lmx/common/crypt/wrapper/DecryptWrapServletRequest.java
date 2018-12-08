package com.lmx.common.crypt.wrapper;


import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Administrator on 2017/8/27.
 */
public class DecryptWrapServletRequest extends HttpServletRequestWrapper {
    private byte[] req;

    public DecryptWrapServletRequest(ServletRequest request, byte[] req) {
        super((HttpServletRequest) request);
        this.req = req;
    }

    public ServletInputStream getInputStream() {
        return new ServletInputStreamWrap(new ByteArrayInputStream(req));
    }

    static class ServletInputStreamWrap extends ServletInputStream {

        private final InputStream sourceStream;


        public ServletInputStreamWrap(InputStream sourceStream) {
            this.sourceStream = sourceStream;
        }


        @Override
        public int read() throws IOException {
            return this.sourceStream.read();
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.sourceStream.close();
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setReadListener(ReadListener readListener) {

        }
    }
}
