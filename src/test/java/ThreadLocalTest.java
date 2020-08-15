import com.alibaba.ttl.TransmittableThreadLocal;
import com.lmx.common.util.PDFUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ribbon做自定义路由时遇到的问题，因为hystrix开启的线程隔离导致线程变量失效，特此记录一下
 * <p>
 * 1.调整为信号量隔离
 * 2.使用以下策略重写ribbon策略和hystrix
 *
 * @author : lucas
 * @date : 2020/07/18 10:55
 */
public class ThreadLocalTest {
    static ExecutorService main = Executors.newFixedThreadPool(1);
    static ExecutorService sub = Executors.newFixedThreadPool(1);
    //    static ThreadLocal<Integer> threadLocal = new InheritableThreadLocal<>();
    static ThreadLocal<Integer> threadLocal = new TransmittableThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {
//        for (int i = 0; i < 8; i++) {
//            threadLocal.set(i);//模拟在servlet拦截器设置上下文值
//            Runnable task = () -> {
//                //模拟在hystrix取上下文值
//                System.out.println(Thread.currentThread() + ",get value=" + threadLocal.get());
//                //释放上下文值
//                threadLocal.remove();
//            };
//            Runnable ttlRunnable = TtlRunnable.get(task);
//            sub.execute(ttlRunnable);
//        }
//        Thread.sleep(Long.MAX_VALUE);


        String fileName = "C:\\Users\\Administrator\\Desktop\\体检报告.pdf";
        PDFUtil pdfUtil = new PDFUtil();
        System.out.println(pdfUtil.readPDF(fileName));
    }
}
